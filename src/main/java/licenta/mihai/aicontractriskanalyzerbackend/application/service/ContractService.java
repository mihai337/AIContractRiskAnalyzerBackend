package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisStatus;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ContractRepository;
import licenta.mihai.aicontractriskanalyzerbackend.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;


    @Transactional
    public ContractEntity upload(String fileName, String sourceUri, String mimeType, String base64Content) {
        ContractEntity entity = ContractEntity.pending(fileName, sourceUri, mimeType, base64Content);
        return contractRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ContractEntity> list() {
        return contractRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ContractEntity getOrThrow(String id) {
        return contractRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Contract not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<ContractEntity> findById(String id) {
        return contractRepository.findById(id);
    }

    @Transactional
    public ContractEntity upsert(ContractEntity payload) {
        ContractEntity entity = contractRepository.findById(payload.getId()).orElse(null);
        if (entity == null) {
            entity = payload;
            if (entity.getUploadedAt() == null) {
                entity.setUploadedAt(Instant.now());
            }
            if (entity.getStatus() == null) {
                entity.setStatus(AnalysisStatus.PENDING);
            }
            return contractRepository.save(entity);
        }

        entity.setFileName(payload.getFileName());
        entity.setSourceUri(payload.getSourceUri());
        entity.setMimeType(payload.getMimeType());
        entity.setBase64Content(payload.getBase64Content());
        entity.setStatus(payload.getStatus());
        entity.setAnalysis(payload.getAnalysis());
        return entity;
    }
}


