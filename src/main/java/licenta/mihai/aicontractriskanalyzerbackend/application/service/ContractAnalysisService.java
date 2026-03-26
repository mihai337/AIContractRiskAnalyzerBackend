package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
public class ContractAnalysisService {

    private final ContractService contractService;
    private final RuleService ruleService;
    private final TextExtractionService textExtractionService;
    private final ClauseDetectionService clauseDetectionService;
    private final MissingClauseDetectionService missingClauseDetectionService;
    private final RuleEngineService ruleEngineService;
    private final RiskScoringService riskScoringService;
    private final SuggestionService suggestionService;
    private final MlInferencePort mlInferencePort;
    private final ApiMapper apiMapper;

    public ContractAnalysisService(
        ContractService contractService,
        RuleService ruleService,
        TextExtractionService textExtractionService,
        ClauseDetectionService clauseDetectionService,
        MissingClauseDetectionService missingClauseDetectionService,
        RuleEngineService ruleEngineService,
        RiskScoringService riskScoringService,
        SuggestionService suggestionService,
        MlInferencePort mlInferencePort,
        ApiMapper apiMapper
    ) {
        this.contractService = contractService;
        this.ruleService = ruleService;
        this.textExtractionService = textExtractionService;
        this.clauseDetectionService = clauseDetectionService;
        this.missingClauseDetectionService = missingClauseDetectionService;
        this.ruleEngineService = ruleEngineService;
        this.riskScoringService = riskScoringService;
        this.suggestionService = suggestionService;
        this.mlInferencePort = mlInferencePort;
        this.apiMapper = apiMapper;
    }

    @Transactional
    public ContractEntity analyze(String contractId, List<String> selectedRuleIds) {
        ContractEntity entity = contractService.getOrThrow(contractId);

        try {
            String extractedText = textExtractionService.extract(entity.getFileName(), entity.getMimeType(), entity.getBase64Content()).text();

            List<ContractAnalysisResult.DetectedClause> detectedClauses = new ArrayList<>(clauseDetectionService.detect(extractedText));
            detectedClauses.addAll(mlInferencePort.refineDetectedClauses(extractedText));

            List<ContractAnalysisResult.MissingClause> missingClauses = missingClauseDetectionService.detect(detectedClauses);
            List<CustomRuleEntity> selectedRules = ruleService.resolveRules(selectedRuleIds);
            List<CustomRule> rules = apiMapper.toCustomRules(selectedRules);
            List<ContractAnalysisResult.RuleAlert> alerts = ruleEngineService.evaluate(extractedText, rules, detectedClauses);
            ContractAnalysisResult.RiskScore riskScore = riskScoringService.calculate(detectedClauses, missingClauses, alerts);
            List<ContractAnalysisResult.AiSuggestion> suggestions = suggestionService.suggest(missingClauses, alerts);

            entity.setAnalysis(new ContractAnalysisResult(
                detectedClauses,
                missingClauses,
                riskScore,
                suggestions,
                alerts,
                Instant.now()
            ));
            entity.setStatus(AnalysisStatus.ANALYZED);
            return entity;
        } catch (RuntimeException ex) {
            entity.setStatus(AnalysisStatus.FAILED);
            throw ex;
        }
    }
}

