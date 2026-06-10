package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ClauseAnalysisEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ClauseEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.DetectedIssueEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ClauseAnalysisRepository;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ClauseRepository;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.DetectedIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClausePersistenceService {

    private final ClauseRepository clauseRepository;
    private final ClauseAnalysisRepository clauseAnalysisRepository;
    private final DetectedIssueRepository detectedIssueRepository;

    public void storeClauses(String contractId, List<ContractAnalysisResult.DetectedClause> clauses, Instant createdAt) {
        List<ClauseEntity> entities = new ArrayList<>();
        for (ContractAnalysisResult.DetectedClause clause : clauses) {
            ClauseEntity entity = new ClauseEntity();
            entity.setId(clause.id());
            entity.setContractId(contractId);
            entity.setClauseType(clause.type());
            entity.setTitle(clause.title() == null ? "" : clause.title());
            entity.setSnippet(clause.snippet() == null ? "" : clause.snippet());
            entity.setConfidence(clause.confidence());
            entity.setRiskLevel(clause.riskLevel());
            entity.setCreatedAt(createdAt);
            entities.add(entity);
        }
        clauseRepository.saveAll(entities);
    }

    public void storeAnalyses(List<LlmAnalysisService.ClauseRiskResult> results, Instant createdAt) {
        List<ClauseAnalysisEntity> analyses = new ArrayList<>();
        List<DetectedIssueEntity> issues = new ArrayList<>();

        for (LlmAnalysisService.ClauseRiskResult result : results) {
            ClauseAnalysisEntity analysis = new ClauseAnalysisEntity();
            String analysisId = UUID.randomUUID().toString();
            analysis.setId(analysisId);
            analysis.setClauseId(result.clauseId());
            analysis.setRiskLevel(result.riskLevel());
            analysis.setRiskScore(result.riskScore());
            analysis.setConfidence(result.confidence());
            analysis.setSummary(result.summary());
            analysis.setRecommendation(result.recommendation());
            analysis.setCreatedAt(createdAt);
            analyses.add(analysis);

            for (LlmAnalysisService.Issue issue : result.issues()) {
                DetectedIssueEntity entity = new DetectedIssueEntity();
                entity.setId(UUID.randomUUID().toString());
                entity.setClauseAnalysisId(analysisId);
                entity.setIssueType(issue.issueType());
                entity.setSeverity(issue.severity());
                entity.setExplanation(issue.explanation());
                entity.setHighlightedText(issue.highlightedText());
                issues.add(entity);
            }
        }

        clauseAnalysisRepository.saveAll(analyses);
        detectedIssueRepository.saveAll(issues);
    }
}

