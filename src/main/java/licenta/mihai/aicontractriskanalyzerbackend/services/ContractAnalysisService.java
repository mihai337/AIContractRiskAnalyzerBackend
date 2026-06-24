package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import licenta.mihai.aicontractriskanalyzerbackend.exceptions.BadRequestException;
import licenta.mihai.aicontractriskanalyzerbackend.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import licenta.mihai.aicontractriskanalyzerbackend.utils.ApiMapper;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml.MlInferencePort;

import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAnalysisService {

    private final ContractService contractService;
    private final RuleService ruleService;
    private final MissingClauseDetectionService missingClauseDetectionService;
    private final RiskAggregationService riskAggregationService;
    private final MlInferencePort mlInferencePort;
    private final ApiMapper apiMapper;
    private final EmbeddingStoreService embeddingStoreService;
    private final LlmAnalysisService llmAnalysisService;
    private final AnalysisPersistenceService analysisPersistenceService;


    public ContractEntity analyze(String contractId, List<String> selectedRuleIds) {
        ContractEntity entity = contractService.getOrThrow(contractId);
        contractService.markPending(contractId, selectedRuleIds);
        entity.setStatus(AnalysisStatus.PENDING);
        entity.setSelectedRuleIds(selectedRuleIds == null ? List.of() : selectedRuleIds);

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
                    new ContractAnalysisResult.RiskScore(0, RiskLevel.LOW, List.of("Document does not appear to be a contract.")),
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
                analysisPersistenceService.persistContract(entity);
                return entity;
            }

            String analysisText = buildAnalysisText(mlResult);

            List<ContractAnalysisResult.DetectedClause> detectedClauses = new ArrayList<>(mlResult.detectedClauses());
            detectedClauses = deduplicateClauses(detectedClauses);

            List<CustomRuleEntity> selectedRules = ruleService.resolveRules(selectedRuleIds);
            List<CustomRule> rules = apiMapper.toCustomRules(selectedRules);
            Set<ClauseType> allowedClauseTypes = resolveAllowedClauseTypes(rules, selectedRuleIds);
            if (selectedRuleIds != null && !selectedRuleIds.isEmpty() && allowedClauseTypes.isEmpty()) {
                log.warn("Selected rules did not resolve to clause types for contract {}. Rule IDs: {}", contractId, selectedRuleIds);
            }
            List<ContractAnalysisResult.DetectedClause> filteredClauses = filterClausesByAllowedTypes(detectedClauses, allowedClauseTypes);
            filteredClauses = collapseDuplicateClauseTypes(filteredClauses);
            log.info("Clause filtering for contract {}: selectedRuleIds={}, allowedClauseTypes={}, before={}, after={}",
                contractId,
                selectedRuleIds,
                allowedClauseTypes,
                detectedClauses.size(),
                filteredClauses.size()
            );

            // Compute embeddings (network) but defer storing them until the final
            // transaction. Querying retrieval before storing also prevents a clause
            // from matching itself / its own siblings as "similar" evidence.
            List<EmbeddingStoreService.ClauseEmbeddingRow> embeddingRows =
                embedClauses(entity.getId(), entity.getOwnerId(), filteredClauses);
            List<List<EmbeddingMatch>> retrievalMatches =
                buildRetrievalMatches(embeddingRows, entity.getOwnerId(), filteredClauses.size());
            Map<ClauseType, String> rulePolicies = buildRulePolicies(rules);
            List<LlmAnalysisService.ClauseRiskResult> llmResults = llmAnalysisService.analyze(
                filteredClauses,
                retrievalMatches,
                contractType,
                rulePolicies
            );
            List<ContractAnalysisResult.ClauseInsight> clauseInsights = buildClauseInsights(llmResults);

            // Missing clauses are exactly the selected rule clause types that weren't detected.
            // A selected clause type therefore lands in either the detected clauses or here.
            Set<ClauseType> detectedTypes = filteredClauses.stream()
                .map(ContractAnalysisResult.DetectedClause::type)
                .collect(Collectors.toSet());
            List<ContractAnalysisResult.MissingClause> missingClauses =
                missingClauseDetectionService.detect(allowedClauseTypes, detectedTypes);
            List<ContractAnalysisResult.RuleAlert> alerts = evaluateRules(analysisText, rules);
            ContractAnalysisResult.RiskScore aggregated = riskAggregationService.aggregate(
                filteredClauses,
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
            List<ContractAnalysisResult.AiSuggestion> suggestions = generateSuggestion(missingClauses, alerts);
            if (allowedClauseTypes == null || allowedClauseTypes.isEmpty()) {
                suggestions.addAll(mlResult.aiSuggestions());
            }

            entity.setAnalysis(new ContractAnalysisResult(
                filteredClauses,
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
            analysisPersistenceService.persistAnalysis(entity, filteredClauses, embeddingRows, llmResults, now);
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

    private List<List<EmbeddingMatch>> buildRetrievalMatches(
        List<EmbeddingStoreService.ClauseEmbeddingRow> rows,
        String ownerId,
        int expected
    ) {
        List<List<EmbeddingMatch>> matches = new ArrayList<>();
        if (rows != null) {
            for (EmbeddingStoreService.ClauseEmbeddingRow row : rows) {
                matches.add(embeddingStoreService.findSimilarClauses(row.embedding(), ownerId, 5));
            }
        }
        while (matches.size() < expected) {
            matches.add(List.of());
        }
        return matches;
    }

    /**
     * Embeds clauses via the ML service (network) without persisting them. The returned
     * rows are stored later, inside the final transaction. Returns an empty list if the
     * embedding service is unavailable so analysis can still proceed.
     */
    private List<EmbeddingStoreService.ClauseEmbeddingRow> embedClauses(
        String contractId,
        String ownerId,
        List<ContractAnalysisResult.DetectedClause> clauses
    ) {
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
            List<EmbeddingStoreService.ClauseEmbeddingRow> rows = new ArrayList<>();
            int limit = Math.min(clauses.size(), embeddings.size());
            for (int i = 0; i < limit; i++) {
                ContractAnalysisResult.DetectedClause clause = clauses.get(i);
                rows.add(new EmbeddingStoreService.ClauseEmbeddingRow(
                    clause.id(),
                    contractId,
                    clause.type().name(),
                    texts.get(i),
                    embeddings.get(i),
                    ownerId
                ));
            }
            return rows;
        } catch (RuntimeException ex) {
            log.warn("Embedding computation skipped for contract {}: {}", contractId, ex.getMessage());
            return List.of();
        }
    }

    private List<ContractAnalysisResult.ClauseInsight> buildClauseInsights(
        List<LlmAnalysisService.ClauseRiskResult> llmResults
    ) {
        if (llmResults == null || llmResults.isEmpty()) {
            return List.of();
        }
        List<ContractAnalysisResult.ClauseInsight> insights = new ArrayList<>();
        for (LlmAnalysisService.ClauseRiskResult result : llmResults) {
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
                issues
            ));
        }
        return insights;
    }

    /**
     * Each selected rule's policy text, keyed by the clause type it targets. The LLM uses
     * this as the standard to judge a clause of that type against (e.g. INTELLECTUAL_PROPERTY
     * -> "Should contain copyrights for product").
     */
    private Map<ClauseType, String> buildRulePolicies(List<CustomRule> rules) {
        Map<ClauseType, String> policies = new LinkedHashMap<>();
        for (CustomRule rule : rules) {
            if (rule.requiredClause() != null && rule.description() != null && !rule.description().isBlank()) {
                policies.putIfAbsent(rule.requiredClause(), rule.description());
            }
        }
        return policies;
    }

    private Set<ClauseType> resolveAllowedClauseTypes(List<CustomRule> rules, List<String> selectedRuleIds) {
        if (selectedRuleIds == null || selectedRuleIds.isEmpty()) {
            return Set.of();
        }
        return rules.stream()
            .map(CustomRule::requiredClause)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private List<ContractAnalysisResult.DetectedClause> filterClausesByAllowedTypes(
        List<ContractAnalysisResult.DetectedClause> clauses,
        Set<ClauseType> allowedClauseTypes
    ) {
        if (allowedClauseTypes == null || allowedClauseTypes.isEmpty()) {
            return clauses;
        }
        return clauses.stream()
            .filter(clause -> allowedClauseTypes.contains(clause.type()))
            .toList();
    }

    private List<ContractAnalysisResult.DetectedClause> collapseDuplicateClauseTypes(
        List<ContractAnalysisResult.DetectedClause> clauses
    ) {
        if (clauses == null || clauses.isEmpty()) {
            return List.of();
        }
        Map<ClauseType, ContractAnalysisResult.DetectedClause> bestByType = new LinkedHashMap<>();
        for (ContractAnalysisResult.DetectedClause clause : clauses) {
            ContractAnalysisResult.DetectedClause current = bestByType.get(clause.type());
            if (current == null || clauseScore(clause) > clauseScore(current)) {
                bestByType.put(clause.type(), clause);
            }
        }
        return new ArrayList<>(bestByType.values());
    }

    private int clauseScore(ContractAnalysisResult.DetectedClause clause) {
        int snippetLength = clause.snippet() == null ? 0 : clause.snippet().trim().length();
        int confidenceScore = (int) Math.round(clause.confidence() * 1000);
        return confidenceScore + Math.min(500, snippetLength);
    }

    private List<ContractAnalysisResult.RuleAlert> evaluateRules(
            String extractedText,
            List<CustomRule> rules
    ) {
        String normalized = extractedText == null ? "" : extractedText.toLowerCase();

        List<ContractAnalysisResult.RuleAlert> alerts = new ArrayList<>();
        for (CustomRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }

            // Clause presence (detected vs missing) is reported separately; rule alerts only
            // flag a required keyword that is absent from the contract text.
            if (rule.keyword() != null && !rule.keyword().isBlank() && !normalized.contains(rule.keyword().toLowerCase())) {
                alerts.add(new ContractAnalysisResult.RuleAlert(
                        rule.id(),
                        rule.name(),
                        "Required keyword is missing: " + rule.keyword(),
                        rule.severity()
                ));
            }
        }

        return alerts;
    }

    private List<ContractAnalysisResult.AiSuggestion> generateSuggestion(
            List<ContractAnalysisResult.MissingClause> missingClauses,
            List<ContractAnalysisResult.RuleAlert> alerts
    ) {
        List<ContractAnalysisResult.AiSuggestion> suggestions = new ArrayList<>();

        for (ContractAnalysisResult.MissingClause missingClause : missingClauses.stream().limit(5).toList()) {
            suggestions.add(new ContractAnalysisResult.AiSuggestion(
                    UUID.randomUUID().toString(),
                    "Add " + missingClause.type() + " clause",
                    "Consider adding a well-defined " + missingClause.type().name().toLowerCase().replace('_', ' ') + " clause.",
                    missingClause.severity()
            ));
        }

        for (ContractAnalysisResult.RuleAlert alert : alerts.stream().limit(5).toList()) {
            suggestions.add(new ContractAnalysisResult.AiSuggestion(
                    UUID.randomUUID().toString(),
                    "Resolve rule alert: " + alert.ruleId(),
                    alert.description(),
                    alert.severity()
            ));
        }

        if (suggestions.isEmpty()) {
            suggestions.add(new ContractAnalysisResult.AiSuggestion(
                    UUID.randomUUID().toString(),
                    "No major issues detected",
                    "Run a legal review for final validation before signing.",
                    RiskLevel.LOW
            ));
        }

        return suggestions;
    }

    public ExtractedText extractTextFromContract(String fileName, String mimeType, String base64Content) {
        MlInferencePort.MlExtractedTextResult mlResult = mlInferencePort.extractText(
                "extract-" + System.currentTimeMillis(),
                fileName,
                mimeType,
                base64Content
        );
        if (!mlResult.success()) {
            throw new BadRequestException("Text extraction failed in ML service");
        }
        return new ExtractedText(mlResult.text(), mlResult.extractionEngine(), mlResult.containsScannedPages());
    }
}
