package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import licenta.mihai.aicontractriskanalyzerbackend.api.mapper.ApiMapper;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.ClauseDetectionService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.MissingClauseDetectionService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.RuleEngineService;
import licenta.mihai.aicontractriskanalyzerbackend.application.engine.SuggestionService;
import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisStatus;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.CustomRule;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.EmbeddingMatch;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAnalysisService {

    private final ContractService contractService;
    private final RuleService ruleService;
    private final ClauseDetectionService clauseDetectionService;
    private final MissingClauseDetectionService missingClauseDetectionService;
    private final RuleEngineService ruleEngineService;
    private final RiskAggregationService riskAggregationService;
    private final SuggestionService suggestionService;
    private final MlInferencePort mlInferencePort;
    private final ApiMapper apiMapper;
    private final EmbeddingStoreService embeddingStoreService;
    private final ClausePersistenceService clausePersistenceService;
    private final LlmAnalysisService llmAnalysisService;


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

            clausePersistenceService.storeClauses(entity.getId(), detectedClauses, now);

            List<List<Double>> embeddings = storeClauseEmbeddings(entity.getId(), detectedClauses);
            List<List<EmbeddingMatch>> retrievalMatches = buildRetrievalMatches(embeddings, detectedClauses.size());
            List<LlmAnalysisService.ClauseRiskResult> llmResults = llmAnalysisService.analyze(
                detectedClauses,
                retrievalMatches,
                contractType
            );
            clausePersistenceService.storeAnalyses(llmResults, now);
            List<ContractAnalysisResult.ClauseInsight> clauseInsights = buildClauseInsights(llmResults, retrievalMatches);

            List<ContractAnalysisResult.MissingClause> missingClauses = missingClauseDetectionService.detect(
                detectedClauses,
                contractType,
                mlResult.isContract()
            );
            List<CustomRuleEntity> selectedRules = ruleService.resolveRules(selectedRuleIds);
            List<CustomRule> rules = apiMapper.toCustomRules(selectedRules);
            List<ContractAnalysisResult.RuleAlert> alerts = ruleEngineService.evaluate(analysisText, rules, detectedClauses);
            ContractAnalysisResult.RiskScore aggregated = riskAggregationService.aggregate(
                detectedClauses,
                missingClauses,
                alerts,
                llmResults,
                retrievalMatches
            );
            List<String> mergedRationale = new ArrayList<>(aggregated.rationale());
            mergedRationale.addAll(mlResult.riskRationale());
            ContractAnalysisResult.RiskScore riskScore = new ContractAnalysisResult.RiskScore(
                aggregated.overallScore(),
                aggregated.riskLevel(),
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
                clauseInsights,
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

    private List<List<EmbeddingMatch>> buildRetrievalMatches(List<List<Double>> embeddings, int expected) {
        List<List<EmbeddingMatch>> matches = new ArrayList<>();
        if (embeddings == null || embeddings.isEmpty()) {
            for (int i = 0; i < expected; i++) {
                matches.add(List.of());
            }
            return matches;
        }
        for (List<Double> embedding : embeddings) {
            matches.add(embeddingStoreService.findSimilarClauses(embedding, 5));
        }
        return matches;
    }

    private List<List<Double>> storeClauseEmbeddings(String contractId, List<ContractAnalysisResult.DetectedClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return List.of();
        }
        List<String> texts = new ArrayList<>();
        for (ContractAnalysisResult.DetectedClause clause : clauses) {
            String snippet = clause.snippet() == null ? "" : clause.snippet().trim();
            if (snippet.isBlank()) {
                snippet = clause.title() == null ? "" : clause.title();
            }
            texts.add(snippet);
        }
        try {
            List<List<Double>> embeddings = mlInferencePort.embedTexts(texts);
            int limit = Math.min(clauses.size(), embeddings.size());
            for (int i = 0; i < limit; i++) {
                ContractAnalysisResult.DetectedClause clause = clauses.get(i);
                String snippet = texts.get(i);
                embeddingStoreService.storeClauseEmbedding(
                    clause.id(),
                    contractId,
                    clause.type().name(),
                    snippet,
                    embeddings.get(i)
                );
            }
            return embeddings;
        } catch (RuntimeException ex) {
            log.warn("Embedding storage skipped for contract {}: {}", contractId, ex.getMessage());
            return List.of();
        }
    }

    private List<ContractAnalysisResult.ClauseInsight> buildClauseInsights(
        List<LlmAnalysisService.ClauseRiskResult> llmResults,
        List<List<EmbeddingMatch>> retrievalMatches
    ) {
        if (llmResults == null || llmResults.isEmpty()) {
            return List.of();
        }
        List<ContractAnalysisResult.ClauseInsight> insights = new ArrayList<>();
        for (int i = 0; i < llmResults.size(); i++) {
            LlmAnalysisService.ClauseRiskResult result = llmResults.get(i);
            List<EmbeddingMatch> evidence = retrievalMatches.size() > i ? retrievalMatches.get(i) : List.of();
            List<ContractAnalysisResult.Issue> issues = result.issues().stream()
                .map(issue -> new ContractAnalysisResult.Issue(
                    issue.issueType(),
                    issue.severity(),
                    issue.explanation(),
                    issue.highlightedText()
                ))
                .toList();
            RiskLevel level = result.riskLevel() == null ? RiskLevel.MEDIUM : result.riskLevel();
            insights.add(new ContractAnalysisResult.ClauseInsight(
                result.clauseId(),
                level,
                result.riskScore(),
                result.confidence(),
                result.summary(),
                result.recommendation(),
                issues,
                evidence
            ));
        }
        return insights;
    }
}
