package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;

import licenta.mihai.aicontractriskanalyzerbackend.models.AnalysisJobStatus;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.AnalysisJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisJobRunner {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContractAnalysisService contractAnalysisService;

    @Async("analysisTaskExecutor")
    public void run(String jobId) {
        AnalysisJobEntity job = analysisJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Analysis job {} not found; skipping", jobId);
            return;
        }

        job.setStatus(AnalysisJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        analysisJobRepository.save(job);

        try {
            contractAnalysisService.analyze(job.getContractId(), job.getSelectedRuleIds());
            job.setStatus(AnalysisJobStatus.COMPLETED);
        } catch (RuntimeException ex) {
            log.warn("Analysis job {} failed: {}", jobId, ex.getMessage(), ex);
            job.setStatus(AnalysisJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
        } finally {
            job.setCompletedAt(Instant.now());
            analysisJobRepository.save(job);
        }
    }
}
