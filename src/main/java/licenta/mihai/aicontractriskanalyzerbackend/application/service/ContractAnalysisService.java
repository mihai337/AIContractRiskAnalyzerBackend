package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.api.mapper.ApiMapper;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.ClauseDetectionService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.MissingClauseDetectionService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.RiskScoringService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.RuleEngineService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.SuggestionService;
import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisStatus;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.CustomRule;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractAnalysisService {

    private final ContractService contractService;
    private final RuleService ruleService;
    private final ClauseDetectionService clauseDetectionService;
    private final MissingClauseDetectionService missingClauseDetectionService;
    private final RuleEngineService ruleEngineService;
    private final RiskScoringService riskScoringService;
    private final SuggestionService suggestionService;
    private final MlInferencePort mlInferencePort;
    private final ApiMapper apiMapper;


    @Transactional
    public ContractEntity analyze(String contractId, List<String> selectedRuleIds) {
        ContractEntity entity = contractService.getOrThrow(contractId);

        try {
            MlInferencePort.MlInferenceResult mlResult = mlInferencePort.analyzeContract(
                entity.getId(),
                entity.getFileName(),
                entity.getMimeType(),
                entity.getBase64Content()
            );
            boolean isContract = Boolean.TRUE.equals(mlResult.isContract());
            String contractType = mlResult.contractType();
            Instant now = Instant.now();

            if (!isContract) {
                entity.setAnalysis(new ContractAnalysisResult(
                    List.of(),
                    List.of(),
                    new ContractAnalysisResult.RiskScore(0, licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel.LOW, List.of("Document does not appear to be a contract.")),
                    List.of(),
                    List.of(),
                    now,
                    contractType,
                    mlResult.contractTypeConfidence(),
                    false,
                    mlResult.nonContractReason()
                ));
                applyMlMetadata(entity, mlResult, selectedRuleIds, now);
                return entity;
            }

            String analysisText = buildAnalysisText(mlResult);

            List<ContractAnalysisResult.DetectedClause> detectedClauses = new ArrayList<>(clauseDetectionService.detect(analysisText));
            detectedClauses.addAll(mlResult.detectedClauses());
            detectedClauses = deduplicateClauses(detectedClauses);

            List<ContractAnalysisResult.MissingClause> missingClauses = missingClauseDetectionService.detect(
                detectedClauses,
                contractType,
                mlResult.isContract()
            );
            List<CustomRuleEntity> selectedRules = ruleService.resolveRules(selectedRuleIds);
            List<CustomRule> rules = apiMapper.toCustomRules(selectedRules);
            List<ContractAnalysisResult.RuleAlert> alerts = ruleEngineService.evaluate(analysisText, rules, detectedClauses);
            ContractAnalysisResult.RiskScore baseRiskScore = riskScoringService.calculate(detectedClauses, missingClauses, alerts);
            List<String> mergedRationale = new ArrayList<>(baseRiskScore.rationale());
            mergedRationale.addAll(mlResult.riskRationale());
            ContractAnalysisResult.RiskScore riskScore = new ContractAnalysisResult.RiskScore(
                baseRiskScore.overallScore(),
                baseRiskScore.riskLevel(),
                mergedRationale
            );
            List<ContractAnalysisResult.AiSuggestion> suggestions = suggestionService.suggest(missingClauses, alerts);
            suggestions.addAll(mlResult.aiSuggestions());

            entity.setAnalysis(new ContractAnalysisResult(
                detectedClauses,
                missingClauses,
                riskScore,
                suggestions,
                alerts,
                now,
                contractType,
                mlResult.contractTypeConfidence(),
                mlResult.isContract(),
                mlResult.nonContractReason()
            ));
            applyMlMetadata(entity, mlResult, selectedRuleIds, now);
            return entity;
        } catch (RuntimeException ex) {
            contractService.markFailed(contractId, ex.getMessage());
            throw ex;
        }
    }

    private void applyMlMetadata(
        ContractEntity entity,
        MlInferencePort.MlInferenceResult mlResult,
        List<String> selectedRuleIds,
        Instant analyzedAt
    ) {
        entity.setMlAnalysisRaw(mlResult.rawPayload());
        entity.setMlEngine(mlResult.engine());
        entity.setMlAnalysisSuccess(mlResult.success());
        entity.setMlAnalyzedAt(analyzedAt);
        entity.setSelectedRuleIds(selectedRuleIds == null ? List.of() : selectedRuleIds);
        entity.setStatus(AnalysisStatus.ANALYZED);
    }

    private List<ContractAnalysisResult.DetectedClause> deduplicateClauses(List<ContractAnalysisResult.DetectedClause> clauses) {
        Map<String, ContractAnalysisResult.DetectedClause> deduped = new LinkedHashMap<>();
        for (ContractAnalysisResult.DetectedClause clause : clauses) {
            String key = clause.type() + "|" + clause.snippet().trim().toLowerCase();
            deduped.putIfAbsent(key, clause);
        }
        return new ArrayList<>(deduped.values());
    }

    private String buildAnalysisText(MlInferencePort.MlInferenceResult mlResult) {
        StringBuilder buffer = new StringBuilder();
        if (mlResult.extractedText() != null && !mlResult.extractedText().isBlank()) {
            buffer.append(mlResult.extractedText()).append('\n');
        }
        for (String line : mlResult.riskRationale()) {
            buffer.append(line).append('\n');
        }
        for (ContractAnalysisResult.AiSuggestion suggestion : mlResult.aiSuggestions()) {
            buffer.append(suggestion.description()).append('\n');
        }
        return buffer.toString();
    }
}
