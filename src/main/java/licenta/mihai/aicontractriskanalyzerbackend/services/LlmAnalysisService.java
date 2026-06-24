package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml.LlmClient;
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.models.EmbeddingMatch;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml.LlmClientProperties;
import licenta.mihai.aicontractriskanalyzerbackend.utils.LlmJsonMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmAnalysisService {

    private final LlmClient llmClient;
    private final LlmJsonMapper llmJsonMapper;
    private final LlmClientProperties llmClientProperties;
    private final PolicyService policyService;

    public List<ClauseRiskResult> analyze(
        List<ContractAnalysisResult.DetectedClause> clauses,
        List<List<EmbeddingMatch>> retrievalMatches,
        String contractType,
        Map<ClauseType, String> rulePolicies
    ) {
        if (clauses == null || clauses.isEmpty()) {
            return List.of();
        }
        String promptTemplate = loadPromptTemplate();
        int concurrency = Math.max(1, Math.min(llmClientProperties.getMaxConcurrency(), clauses.size()));
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        try {
            List<Future<ClauseRiskResult>> futures = new ArrayList<>();
            for (int i = 0; i < clauses.size(); i++) {
                ContractAnalysisResult.DetectedClause clause = clauses.get(i);
                List<EmbeddingMatch> matches = retrievalMatches.size() > i ? retrievalMatches.get(i) : List.of();
                Callable<ClauseRiskResult> task = () -> analyzeClause(promptTemplate, clause, matches, contractType, rulePolicies);
                futures.add(executor.submit(task));
            }

            List<ClauseRiskResult> results = new ArrayList<>(futures.size());
            for (Future<ClauseRiskResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException ex) {
                    // analyzeClause only propagates when failOpen is disabled.
                    Throwable cause = ex.getCause();
                    throw new IllegalStateException("LLM clause analysis failed", cause == null ? ex : cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("LLM clause analysis interrupted", ex);
                }
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private ClauseRiskResult analyzeClause(
        String promptTemplate,
        ContractAnalysisResult.DetectedClause clause,
        List<EmbeddingMatch> matches,
        String contractType,
        Map<ClauseType, String> rulePolicies
    ) {
        String prompt = buildPrompt(promptTemplate, clause, matches, contractType, rulePolicies);
        try {
            String json = llmClient.completeJson(prompt);
            try {
                ClauseRiskResult parsed = llmJsonMapper.toClauseRiskResult(clause.id(), json);
                return applyAbstention(sanitizeIssues(clause, parsed));
            } catch (RuntimeException ex) {
                log.warn("LLM JSON parse failed for clause {}: {}", clause.id(), truncate(json));
                if (!llmClientProperties.isFailOpen()) {
                    throw ex;
                }
                return applyAbstention(buildFallback(clause));
            }
        } catch (RuntimeException ex) {
            if (!llmClientProperties.isFailOpen()) {
                throw ex;
            }
            return applyAbstention(buildFallback(clause));
        }
    }

    /**
     * Drops a hallucinated "highlightedText" quote that does not actually appear in the
     * source clause. The issue itself is kept (the explanation may still be valid), but the
     * unverifiable quote is cleared so the UI never shows fabricated evidence.
     */
    private ClauseRiskResult sanitizeIssues(ContractAnalysisResult.DetectedClause clause, ClauseRiskResult result) {
        if (result.issues().isEmpty()) {
            return result;
        }
        String source = normalize(clause.snippet());
        List<Issue> sanitized = new ArrayList<>(result.issues().size());
        for (Issue issue : result.issues()) {
            String highlighted = issue.highlightedText();
            if (highlighted == null || highlighted.isBlank() || source.contains(normalize(highlighted))) {
                sanitized.add(issue);
            } else {
                log.debug("Dropping unverifiable highlight for clause {}: {}", clause.id(), truncate(highlighted));
                sanitized.add(new Issue(issue.issueType(), issue.severity(), issue.explanation(), ""));
            }
        }
        return new ClauseRiskResult(
            result.clauseId(),
            result.riskLevel(),
            result.riskScore(),
            result.confidence(),
            result.summary(),
            result.recommendation(),
            sanitized
        );
    }

    /**
     * Flags low-confidence assessments for manual review instead of presenting them as
     * trustworthy. The risk level/score are preserved but the text makes the uncertainty clear.
     */
    private ClauseRiskResult applyAbstention(ClauseRiskResult result) {
        if (result.confidence() >= llmClientProperties.getAbstainConfidenceThreshold()) {
            return result;
        }
        String summary = ("Low confidence — manual review recommended. "
            + (result.summary() == null ? "" : result.summary())).trim();
        String recommendation = ("Have a qualified reviewer assess this clause manually. "
            + (result.recommendation() == null ? "" : result.recommendation())).trim();
        return new ClauseRiskResult(
            result.clauseId(),
            result.riskLevel(),
            result.riskScore(),
            result.confidence(),
            summary,
            recommendation,
            result.issues()
        );
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ");
        return oneLine.length() > 500 ? oneLine.substring(0, 500) + "..." : oneLine;
    }

    private String buildPrompt(
        String template,
        ContractAnalysisResult.DetectedClause clause,
        List<EmbeddingMatch> matches,
        String contractType,
        Map<ClauseType, String> rulePolicies
    ) {
        // Prefer the policy the user attached to the rule for this clause type; otherwise
        // fall back to the seeded default policy.
        String rulePolicy = rulePolicies == null ? null : rulePolicies.get(clause.type());
        String policy = (rulePolicy != null && !rulePolicy.isBlank())
            ? rulePolicy
            : policyService.policyForClauseType(clause.type(), contractType);
        String retrieved = matches.stream()
            .limit(3)
            .map(match -> "- " + match.snippet())
            .reduce("", (a, b) -> a + "\n" + b);

        return template
            .replace("{policy}", policy.isBlank() ? "No policy available." : policy)
            .replace("{clause}", clause.snippet() == null ? "" : clause.snippet())
            .replace("{retrieved}", retrieved.isBlank() ? "- none" : retrieved)
            .replace("{contractType}", contractType == null ? "" : contractType);
    }

    private String loadPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/clause-risk.txt");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }

    private ClauseRiskResult buildFallback(ContractAnalysisResult.DetectedClause clause) {
        RiskLevel level = clause.riskLevel() == null ? RiskLevel.MEDIUM : clause.riskLevel();
        int score = switch (level) {
            case LOW -> 20;
            case MEDIUM -> 45;
            case HIGH -> 70;
            case CRITICAL -> 90;
        };
        return new ClauseRiskResult(
            clause.id(),
            level,
            score,
            // Deliberately low so applyAbstention flags the fallback for manual review.
            0.2,
            "Fallback risk assessment used.",
            "Review this clause manually.",
            List.of()
        );
    }

    public record ClauseRiskResult(
        String clauseId,
        RiskLevel riskLevel,
        int riskScore,
        double confidence,
        String summary,
        String recommendation,
        List<Issue> issues
    ) {
    }

    public record Issue(
        String issueType,
        RiskLevel severity,
        String explanation,
        String highlightedText
    ) {
    }
}
