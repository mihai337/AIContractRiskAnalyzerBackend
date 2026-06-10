package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.models.EmbeddingMatch;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;
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

            double classifier = Math.clamp(clause.confidence(),0,1);
            double retrieval = evidence.isEmpty() ? 0.5 : Math.clamp(1 - evidence.getFirst().distance(),0,1);
            double llmConf = Math.clamp(llm == null ? 0.5 : llm.confidence(),0,1);

            double blendedConfidence = 0.4 * classifier + 0.3 * retrieval + 0.3 * llmConf;
            int baseScore = BASE_SCORES.getOrDefault(clause.riskLevel(), 45);
            int llmScore = llm == null ? baseScore : Math.clamp(llm.riskScore(),0,100);

            double clauseScore = (llmScore * blendedConfidence) + (baseScore * (1 - blendedConfidence));
            scoreSum += clauseScore;
        }

        double avgScore = scoreSum / clauses.size();
        int penalty = computePenalty(missingClauses, alerts);
        int finalScore = Math.clamp((int) Math.round(avgScore + penalty),0,100);

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
}

