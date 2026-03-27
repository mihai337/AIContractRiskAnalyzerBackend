package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.AnalysisJobRepository;
import licenta.mihai.aicontractriskanalyzerbackend.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContractService contractService;
    private final AnalysisJobRunner analysisJobRunner;

    public AnalysisJobService(
        AnalysisJobRepository analysisJobRepository,
        ContractService contractService,
        AnalysisJobRunner analysisJobRunner
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.contractService = contractService;
        this.analysisJobRunner = analysisJobRunner;
    }

    @Transactional
    public AnalysisJobEntity createJob(String contractId, List<String> selectedRuleIds) {
        contractService.getOrThrow(contractId);
        AnalysisJobEntity job = analysisJobRepository.save(AnalysisJobEntity.pending(contractId, selectedRuleIds));
        analysisJobRunner.runAsync(job.getId());
        return job;
    }

    @Transactional(readOnly = true)
    public AnalysisJobEntity getJobOrThrow(String contractId, String jobId) {
        return analysisJobRepository.findByIdAndContractId(jobId, contractId)
            .orElseThrow(() -> new NotFoundException("Analysis job not found: " + jobId));
    }
}

