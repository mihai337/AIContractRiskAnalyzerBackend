package licenta.mihai.aicontractriskanalyzerbackend.api.mapper;

import java.util.Collections;
import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.AiSuggestionDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.AnalysisJobDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ClauseAnalysisDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ContractAnalysisDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ContractRecordDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.CustomRuleDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ExtractTextResponseDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.MissingClauseDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.RiskScoreDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.RuleAlertDto;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.CustomRule;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ExtractedText;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {

    public ContractRecordDto toContractRecordDto(ContractEntity entity) {
        return new ContractRecordDto(
            entity.getId(),
            entity.getFileName(),
            entity.getSourceUri(),
            entity.getUploadedAt().getEpochSecond(),
            entity.getStatus(),
            toContractAnalysisDto(entity.getAnalysis()),
            safeList(entity.getSelectedRuleIds())
        );
    }

    public ContractAnalysisDto toContractAnalysisDto(ContractAnalysisResult result) {
        if (result == null) {
            return null;
        }
        ContractAnalysisResult.RiskScore riskScore = result.riskScore() == null
            ? defaultRiskScore()
            : result.riskScore();
        return new ContractAnalysisDto(
            safeList(result.detectedClauses()).stream()
                .map(c -> new ClauseAnalysisDto(c.id(), c.type(), c.title(), c.snippet(), c.confidence(), c.riskLevel()))
                .toList(),
            safeList(result.missingClauses()).stream().map(m -> new MissingClauseDto(m.type(), m.reason(), m.severity())).toList(),
            new RiskScoreDto(riskScore.overallScore(), riskScore.riskLevel(), safeList(riskScore.rationale())),
            safeList(result.aiSuggestions()).stream().map(s -> new AiSuggestionDto(s.id(), s.title(), s.description(), s.priority())).toList(),
            safeList(result.ruleAlerts()).stream().map(r -> new RuleAlertDto(r.ruleId(), r.title(), r.description(), r.severity())).toList(),
            result.generatedAt() == null ? java.time.Instant.now().getEpochSecond() : result.generatedAt().getEpochSecond(),
            result.contractType(),
            result.contractTypeConfidence(),
            result.isContract(),
            result.nonContractReason()
        );
    }

    public ExtractTextResponseDto toExtractTextResponseDto(ExtractedText extractedText) {
        return new ExtractTextResponseDto(extractedText.text(), extractedText.extractionEngine(), extractedText.containsScannedPages());
    }

    public CustomRuleDto toCustomRuleDto(CustomRuleEntity entity) {
        return new CustomRuleDto(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getRequiredClause(),
            entity.getKeyword(),
            entity.getSeverity(),
            entity.isEnabled()
        );
    }

    public CustomRuleEntity toCustomRuleEntity(CustomRuleDto dto) {
        CustomRuleEntity entity = new CustomRuleEntity();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setRequiredClause(dto.requiredClause());
        entity.setKeyword(dto.keyword());
        entity.setSeverity(dto.severity());
        entity.setEnabled(dto.enabled());
        return entity;
    }

    public CustomRule toCustomRule(CustomRuleEntity entity) {
        return new CustomRule(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getRequiredClause(),
            entity.getKeyword(),
            entity.getSeverity(),
            entity.isEnabled()
        );
    }

    public List<CustomRule> toCustomRules(List<CustomRuleEntity> entities) {
        return entities.stream().map(this::toCustomRule).toList();
    }

    public ContractEntity toContractEntity(ContractRecordDto dto, String mimeType, String base64Content) {
        ContractEntity entity = new ContractEntity();
        entity.setId(dto.id());
        entity.setFileName(dto.fileName());
        entity.setSourceUri(dto.sourceUri());
        entity.setMimeType(mimeType);
        entity.setBase64Content(base64Content);
        entity.setUploadedAt(java.time.Instant.ofEpochSecond(dto.uploadedAtEpochSeconds()));
        entity.setStatus(dto.status());
        entity.setAnalysis(toContractAnalysis(dto.analysis()));
        entity.setSelectedRuleIds(safeList(dto.selectedRuleIds()));
        return entity;
    }

    public ContractAnalysisResult toContractAnalysis(ContractAnalysisDto dto) {
        if (dto == null) {
            return null;
        }
        RiskScoreDto riskScore = dto.riskScore() == null
            ? new RiskScoreDto(0, licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel.CRITICAL, List.of("Risk score unavailable"))
            : dto.riskScore();
        return new ContractAnalysisResult(
            safeList(dto.detectedClauses()).stream()
                .map(c -> new ContractAnalysisResult.DetectedClause(c.id(), c.type(), c.title(), c.snippet(), c.confidence(), c.riskLevel()))
                .toList(),
            safeList(dto.missingClauses()).stream()
                .map(m -> new ContractAnalysisResult.MissingClause(m.type(), m.reason(), m.severity()))
                .toList(),
            new ContractAnalysisResult.RiskScore(
                riskScore.overallScore(),
                riskScore.riskLevel(),
                safeList(riskScore.rationale())
            ),
            safeList(dto.aiSuggestions()).stream()
                .map(s -> new ContractAnalysisResult.AiSuggestion(s.id(), s.title(), s.description(), s.priority()))
                .toList(),
            safeList(dto.ruleAlerts()).stream()
                .map(r -> new ContractAnalysisResult.RuleAlert(r.ruleId(), r.title(), r.description(), r.severity()))
                .toList(),
            java.time.Instant.ofEpochSecond(dto.generatedAtEpochSeconds()),
            dto.contractType(),
            dto.contractTypeConfidence(),
            dto.isContract(),
            dto.nonContractReason()
        );
    }

    private ContractAnalysisResult.RiskScore defaultRiskScore() {
        return new ContractAnalysisResult.RiskScore(
            0,
            licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel.CRITICAL,
            List.of("Risk score unavailable")
        );
    }

    private <T> List<T> safeList(List<T> input) {
        return input == null ? Collections.emptyList() : input;
    }

    public AnalysisJobDto toAnalysisJobDto(AnalysisJobEntity entity) {
        return new AnalysisJobDto(
            entity.getId(),
            entity.getContractId(),
            entity.getStatus(),
            entity.getCreatedAt().getEpochSecond(),
            entity.getStartedAt() == null ? null : entity.getStartedAt().getEpochSecond(),
            entity.getCompletedAt() == null ? null : entity.getCompletedAt().getEpochSecond(),
            entity.getErrorMessage()
        );
    }
}
