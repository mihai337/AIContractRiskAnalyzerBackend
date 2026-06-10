package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.models.AnalysisStatus;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ClauseAnalysisEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ClauseEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ClauseAnalysisRepository;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ClauseEmbeddingRepository;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ClauseRepository;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ContractRepository;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.DetectedIssueRepository;
import licenta.mihai.aicontractriskanalyzerbackend.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ClauseRepository clauseRepository;
    private final ClauseAnalysisRepository clauseAnalysisRepository;
    private final DetectedIssueRepository detectedIssueRepository;
    private final ClauseEmbeddingRepository clauseEmbeddingRepository;


    @Transactional
    public ContractEntity upload(String fileName, String sourceUri, String mimeType, String base64Content, String ownerId) {
        ContractEntity entity = ContractEntity.pending(fileName, sourceUri, mimeType, base64Content, ownerId);
        return contractRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ContractEntity> list(String ownerId) {
        return contractRepository.findByOwnerIdOrderByUploadedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public ContractEntity getOrThrow(String id, String ownerId) {
        return contractRepository.findByIdAndOwnerId(id, ownerId)
            .orElseThrow(() -> new NotFoundException("Contract not found: " + id));
    }

    @Transactional(readOnly = true)
    public ContractEntity getOrThrow(String id) {
        return contractRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Contract not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<ContractEntity> findById(String id, String ownerId) {
        return contractRepository.findByIdAndOwnerId(id, ownerId);
    }

    @Transactional
    public ContractEntity upsert(ContractEntity payload, String ownerId) {
        ContractEntity entity = contractRepository.findByIdAndOwnerId(payload.getId(), ownerId).orElse(null);
        if (entity == null) {
            entity = payload;
            entity.setOwnerId(ownerId);
            if (entity.getUploadedAt() == null) {
                entity.setUploadedAt(Instant.now());
            }
            if (entity.getStatus() == null) {
                entity.setStatus(AnalysisStatus.PENDING);
            }
            if (entity.getSelectedRuleIds() == null) {
                entity.setSelectedRuleIds(List.of());
            }
            return contractRepository.save(entity);
        }

        entity.setFileName(payload.getFileName());
        entity.setSourceUri(payload.getSourceUri());
        entity.setMimeType(payload.getMimeType());
        entity.setBase64Content(payload.getBase64Content());
        entity.setStatus(payload.getStatus());
        entity.setAnalysis(payload.getAnalysis());
        entity.setSelectedRuleIds(payload.getSelectedRuleIds());
        return entity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String contractId, String errorMessage) {
        ContractEntity entity = contractRepository.findById(contractId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setStatus(AnalysisStatus.FAILED);
        entity.setMlAnalysisSuccess(Boolean.FALSE);
        entity.setMlAnalyzedAt(Instant.now());
        if (errorMessage != null && !errorMessage.isBlank()) {
            entity.setMlAnalysisRaw("{\"error\":\"" + errorMessage.replace("\"", "'") + "\"}");
        }
        contractRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPending(String contractId, List<String> selectedRuleIds) {
        ContractEntity entity = contractRepository.findById(contractId).orElse(null);
        if (entity == null) {
            return;
        }
        entity.setStatus(AnalysisStatus.PENDING);
        entity.setSelectedRuleIds(selectedRuleIds == null ? List.of() : selectedRuleIds);
        contractRepository.saveAndFlush(entity);
    }

    @Transactional
    public void delete(String contractId, String ownerId) {
        ContractEntity entity = getOrThrow(contractId, ownerId);
        List<ClauseEntity> clauses = clauseRepository.findByContractId(entity.getId());
        List<String> clauseIds = clauses.stream().map(ClauseEntity::getId).toList();
        if (!clauseIds.isEmpty()) {
            List<ClauseAnalysisEntity> analyses = clauseAnalysisRepository.findByClauseIdIn(clauseIds);
            List<String> analysisIds = analyses.stream().map(ClauseAnalysisEntity::getId).toList();
            if (!analysisIds.isEmpty()) {
                detectedIssueRepository.deleteByClauseAnalysisIdIn(analysisIds);
            }
            clauseAnalysisRepository.deleteByClauseIdIn(clauseIds);
        }
        clauseRepository.deleteByContractId(entity.getId());
        try {
            clauseEmbeddingRepository.deleteByContractId(entity.getId());
        } catch (RuntimeException ignored) {
            // Embed store may be unavailable in tests or when pgvector is not configured.
        }
        contractRepository.deleteByIdAndOwnerId(entity.getId(), ownerId);
    }
}
