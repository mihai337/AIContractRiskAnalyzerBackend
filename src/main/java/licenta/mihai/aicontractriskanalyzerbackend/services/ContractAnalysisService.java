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
import org.springframework.transaction.annotation.Transactional;

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
    private final ClausePersistenceService clausePersistenceService;
    private final LlmAnalysisService llmAnalysisService;


    @Transactional
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

            clausePersistenceService.storeClauses(entity.getId(), filteredClauses, now);

            List<List<Double>> embeddings = storeClauseEmbeddings(entity.getId(), filteredClauses);
            List<List<EmbeddingMatch>> retrievalMatches = buildRetrievalMatches(embeddings, filteredClauses.size());
            List<LlmAnalysisService.ClauseRiskResult> llmResults = llmAnalysisService.analyze(
                filteredClauses,
                retrievalMatches,
                contractType
            );
            clausePersistenceService.storeAnalyses(llmResults, now);
            List<ContractAnalysisResult.ClauseInsight> clauseInsights = buildClauseInsights(llmResults, retrievalMatches);

            List<ContractAnalysisResult.MissingClause> missingClauses = missingClauseDetectionService.detect(
                filteredClauses,
                contractType,
                mlResult.isContract()
            );
            missingClauses = filterMissingClausesByAllowedTypes(missingClauses, allowedClauseTypes);
            List<ContractAnalysisResult.RuleAlert> alerts = evaluateRules(analysisText, rules, filteredClauses);
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
                dedupeEvidence(evidence)
            ));
        }
        return insights;
    }

    private List<EmbeddingMatch> dedupeEvidence(List<EmbeddingMatch> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<EmbeddingMatch> unique = new ArrayList<>();
        for (EmbeddingMatch match : evidence) {
            String snippet = match.snippet() == null ? "" : match.snippet().trim();
            String clauseType = match.clauseType() == null ? "" : match.clauseType().trim();
            String key = clauseType + "|" + snippet;
            if (seen.add(key)) {
                unique.add(match);
            }
        }
        return unique;
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

    private List<ContractAnalysisResult.MissingClause> filterMissingClausesByAllowedTypes(
        List<ContractAnalysisResult.MissingClause> missingClauses,
        Set<ClauseType> allowedClauseTypes
    ) {
        if (allowedClauseTypes == null || allowedClauseTypes.isEmpty()) {
            return missingClauses;
        }
        return missingClauses.stream()
            .filter(missing -> allowedClauseTypes.contains(missing.type()))
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
            List<CustomRule> rules,
            List<ContractAnalysisResult.DetectedClause> detectedClauses
    ) {
        String normalized = extractedText == null ? "" : extractedText.toLowerCase();
        Set<ClauseType> detectedTypes = detectedClauses.stream().map(ContractAnalysisResult.DetectedClause::type)
                .collect(java.util.stream.Collectors.toSet());

        List<ContractAnalysisResult.RuleAlert> alerts = new ArrayList<>();
        for (CustomRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }

            if (rule.keyword() != null && !rule.keyword().isBlank() && !normalized.contains(rule.keyword().toLowerCase())) {
                alerts.add(new ContractAnalysisResult.RuleAlert(
                        rule.id(),
                        rule.name(),
                        "Required keyword is missing: " + rule.keyword(),
                        rule.severity()
                ));
            }

            if (rule.requiredClause() != null && !detectedTypes.contains(rule.requiredClause())) {
                alerts.add(new ContractAnalysisResult.RuleAlert(
                        rule.id(),
                        rule.name(),
                        "Required clause is missing: " + rule.requiredClause(),
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
