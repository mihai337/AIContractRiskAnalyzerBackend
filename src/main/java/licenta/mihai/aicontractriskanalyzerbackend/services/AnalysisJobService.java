package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.List;

import licenta.mihai.aicontractriskanalyzerbackend.models.AnalysisJobStatus;
import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.AnalysisJobRepository;
import licenta.mihai.aicontractriskanalyzerbackend.exceptions.NotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContractService contractService;
    private final ContractAnalysisService contractAnalysisService;


    @Transactional
    public AnalysisJobEntity createJob(String contractId, List<String> selectedRuleIds, String ownerId) {
        contractService.getOrThrow(contractId, ownerId);
        AnalysisJobEntity job = analysisJobRepository.save(AnalysisJobEntity.pending(contractId, selectedRuleIds));
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runAsync(job.getId());
                }
            });
        } else {
            runAsync(job.getId());
        }
        return job;
    }

    @Transactional(readOnly = true)
    public AnalysisJobEntity getJobOrThrow(String contractId, String jobId, String ownerId) {
        contractService.getOrThrow(contractId, ownerId);
        return analysisJobRepository.findByIdAndContractId(jobId, contractId)
            .orElseThrow(() -> new NotFoundException("Analysis job not found: " + jobId));
    }

    @Async("analysisTaskExecutor")
    public void runAsync(String jobId) {
        AnalysisJobEntity job = analysisJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setStatus(AnalysisJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        analysisJobRepository.save(job);

        try {
            contractAnalysisService.analyze(job.getContractId(), job.getSelectedRuleIds());
            job.setStatus(AnalysisJobStatus.COMPLETED);
        } catch (RuntimeException ex) {
            job.setStatus(AnalysisJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
        } finally {
            job.setCompletedAt(Instant.now());
            analysisJobRepository.save(job);
        }
    }
}
