package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.util.ArrayList;
import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.EmbeddingMatch;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.llm.LlmClientProperties;
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
        String contractType
    ) {
        List<ClauseRiskResult> results = new ArrayList<>();
        String promptTemplate = loadPromptTemplate();

        for (int i = 0; i < clauses.size(); i++) {
            ContractAnalysisResult.DetectedClause clause = clauses.get(i);
            List<EmbeddingMatch> matches = retrievalMatches.size() > i ? retrievalMatches.get(i) : List.of();
            String prompt = buildPrompt(promptTemplate, clause, matches, contractType);
            try {
                String json = llmClient.completeJson(prompt);
                try {
                    results.add(llmJsonMapper.toClauseRiskResult(clause.id(), json));
                } catch (RuntimeException ex) {
                    String snippet = json == null ? "" : json.replaceAll("\n", " ");
                    if (snippet.length() > 500) {
                        snippet = snippet.substring(0, 500) + "...";
                    }
                    log.warn("LLM JSON parse failed for clause {}: {}", clause.id(), snippet);
                    if (!llmClientProperties.isFailOpen()) {
                        throw ex;
                    }
                    results.add(buildFallback(clause));
                }
            } catch (RuntimeException ex) {
                if (!llmClientProperties.isFailOpen()) {
                    throw ex;
                }
                results.add(buildFallback(clause));
            }
        }

        return results;
    }

    private String buildPrompt(
        String template,
        ContractAnalysisResult.DetectedClause clause,
        List<EmbeddingMatch> matches,
        String contractType
    ) {
        String policy = policyService.policyForClauseType(clause.type(), contractType);
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
            Math.max(0.4, clause.confidence()),
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
