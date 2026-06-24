package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.AnalysisJobRepository;
import licenta.mihai.aicontractriskanalyzerbackend.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContractService contractService;
    private final AnalysisJobRunner analysisJobRunner;


    @Transactional
    public AnalysisJobEntity createJob(String contractId, List<String> selectedRuleIds, String ownerId) {
        contractService.getOrThrow(contractId, ownerId);
        AnalysisJobEntity job = analysisJobRepository.save(AnalysisJobEntity.pending(contractId, selectedRuleIds));
        // Run after the create transaction commits so the runner can read the persisted job.
        // The call goes through a separate bean so @Async is honoured (a self-invoked @Async
        // method would run synchronously and not persist its status updates).
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    analysisJobRunner.run(job.getId());
                }
            });
        } else {
            analysisJobRunner.run(job.getId());
        }
        return job;
    }

    @Transactional(readOnly = true)
    public AnalysisJobEntity getJobOrThrow(String contractId, String jobId, String ownerId) {
        contractService.getOrThrow(contractId, ownerId);
        return analysisJobRepository.findByIdAndContractId(jobId, contractId)
            .orElseThrow(() -> new NotFoundException("Analysis job not found: " + jobId));
    }
}
