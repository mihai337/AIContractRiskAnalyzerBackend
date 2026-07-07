package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.List;

import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.AnalysisJobRepository;
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
}
