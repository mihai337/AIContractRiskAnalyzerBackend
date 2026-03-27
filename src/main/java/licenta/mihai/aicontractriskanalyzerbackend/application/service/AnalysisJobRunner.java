package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.time.Instant;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisJobStatus;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.AnalysisJobRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisJobRunner {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContractAnalysisService contractAnalysisService;

    public AnalysisJobRunner(AnalysisJobRepository analysisJobRepository, ContractAnalysisService contractAnalysisService) {
        this.analysisJobRepository = analysisJobRepository;
        this.contractAnalysisService = contractAnalysisService;
    }

    @Async("analysisTaskExecutor")
    @Transactional
    public void runAsync(String jobId) {
        AnalysisJobEntity job = analysisJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setStatus(AnalysisJobStatus.RUNNING);
        job.setStartedAt(Instant.now());

        try {
            contractAnalysisService.analyze(job.getContractId(), job.getSelectedRuleIds());
            job.setStatus(AnalysisJobStatus.COMPLETED);
        } catch (RuntimeException ex) {
            job.setStatus(AnalysisJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
        } finally {
            job.setCompletedAt(Instant.now());
        }
    }
}

