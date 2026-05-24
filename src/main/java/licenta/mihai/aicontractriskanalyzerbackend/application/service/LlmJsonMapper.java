package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class LlmJsonMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public LlmAnalysisService.ClauseRiskResult toClauseRiskResult(String clauseId, String json) {
        try {
            LlmClauseResponse response = OBJECT_MAPPER.readValue(json, LlmClauseResponse.class);
            return toClauseRiskResult(clauseId, response);
        } catch (Exception ex) {
            try {
                String extracted = extractJsonObject(json);
                String normalized = normalizeJsonString(extracted);
                LlmClauseResponse response = OBJECT_MAPPER.readValue(normalized, LlmClauseResponse.class);
                return toClauseRiskResult(clauseId, response);
            } catch (Exception nested) {
                throw new IllegalStateException("LLM response could not be parsed", ex);
            }
        }
    }

    private String normalizeJsonString(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                return OBJECT_MAPPER.readValue(trimmed, String.class);
            } catch (Exception ignored) {
                return trimmed;
            }
        }
        return trimmed;
    }

    private LlmAnalysisService.ClauseRiskResult toClauseRiskResult(String clauseId, LlmClauseResponse response) {
        RiskLevel riskLevel = parseRisk(response.riskLevel);
        List<LlmAnalysisService.Issue> issues = new ArrayList<>();
        if (response.issues != null) {
            for (LlmIssue issue : response.issues) {
                issues.add(new LlmAnalysisService.Issue(
                    issue.issueType == null ? "UNKNOWN" : issue.issueType,
                    parseRisk(issue.severity),
                    issue.explanation == null ? "" : issue.explanation,
                    issue.highlightedText == null ? "" : issue.highlightedText
                ));
            }
        }
        double confidence = response.confidence <= 0 ? 0.5 : response.confidence;
        int riskScore = response.riskScore <= 0 ? 50 : response.riskScore;
        return new LlmAnalysisService.ClauseRiskResult(
            clauseId,
            riskLevel,
            clampScore(riskScore),
            Math.max(0.0, Math.min(1.0, confidence)),
            response.summary == null ? "" : response.summary,
            response.recommendation == null ? "" : response.recommendation,
            issues
        );
    }

    private String extractJsonObject(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private RiskLevel parseRisk(String raw) {
        if (raw == null) {
            return RiskLevel.MEDIUM;
        }
        try {
            return RiskLevel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return RiskLevel.MEDIUM;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmClauseResponse {
        public String riskLevel;
        public int riskScore;
        public double confidence;
        public String summary;
        public String recommendation;
        public List<LlmIssue> issues;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmIssue {
        public String issueType;
        public String severity;
        public String explanation;
        public String highlightedText;
    }
}