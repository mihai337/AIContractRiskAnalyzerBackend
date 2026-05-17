package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.EmbeddingMatch;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class RiskAggregationService {

    private static final Map<RiskLevel, Integer> BASE_SCORES = Map.of(
        RiskLevel.LOW, 20,
        RiskLevel.MEDIUM, 45,
        RiskLevel.HIGH, 70,
        RiskLevel.CRITICAL, 90
    );

    public ContractAnalysisResult.RiskScore aggregate(
        List<ContractAnalysisResult.DetectedClause> clauses,
        List<ContractAnalysisResult.MissingClause> missingClauses,
        List<ContractAnalysisResult.RuleAlert> alerts,
        List<LlmAnalysisService.ClauseRiskResult> llmResults,
        List<List<EmbeddingMatch>> retrievalMatches
    ) {
        if (clauses == null || clauses.isEmpty()) {
            return new ContractAnalysisResult.RiskScore(0, RiskLevel.LOW, List.of("No clauses detected"));
        }

        List<String> rationale = new ArrayList<>();
        double scoreSum = 0;

        for (int i = 0; i < clauses.size(); i++) {
            ContractAnalysisResult.DetectedClause clause = clauses.get(i);
            LlmAnalysisService.ClauseRiskResult llm = llmResults.size() > i ? llmResults.get(i) : null;
            List<EmbeddingMatch> evidence = retrievalMatches.size() > i ? retrievalMatches.get(i) : List.of();

            double classifier = clamp01(clause.confidence());
            double retrieval = evidence.isEmpty() ? 0.5 : clamp01(1 - evidence.get(0).distance());
            double llmConf = clamp01(llm == null ? 0.5 : llm.confidence());

            double blendedConfidence = 0.4 * classifier + 0.3 * retrieval + 0.3 * llmConf;
            int baseScore = BASE_SCORES.getOrDefault(clause.riskLevel(), 45);
            int llmScore = llm == null ? baseScore : clampScore(llm.riskScore());

            double clauseScore = (llmScore * blendedConfidence) + (baseScore * (1 - blendedConfidence));
            scoreSum += clauseScore;
        }

        double avgScore = scoreSum / clauses.size();
        int penalty = computePenalty(missingClauses, alerts);
        int finalScore = clampScore((int) Math.round(avgScore + penalty));

        rationale.add("Blend: 0.4 classifier, 0.3 retrieval, 0.3 LLM confidence");
        if (penalty > 0) {
            rationale.add("Penalty applied from missing clauses and alerts: +" + penalty);
        }

        return new ContractAnalysisResult.RiskScore(finalScore, RiskLevel.fromScore(finalScore), rationale);
    }

    private int computePenalty(
        List<ContractAnalysisResult.MissingClause> missingClauses,
        List<ContractAnalysisResult.RuleAlert> alerts
    ) {
        int penalty = 0;
        if (missingClauses != null) {
            for (ContractAnalysisResult.MissingClause missing : missingClauses) {
                penalty += severityPenalty(missing.severity());
            }
        }
        if (alerts != null) {
            for (ContractAnalysisResult.RuleAlert alert : alerts) {
                penalty += severityPenalty(alert.severity());
            }
        }
        return penalty;
    }

    private int severityPenalty(RiskLevel level) {
        return switch (level) {
            case LOW -> 2;
            case MEDIUM -> 5;
            case HIGH -> 8;
            case CRITICAL -> 12;
        };
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

