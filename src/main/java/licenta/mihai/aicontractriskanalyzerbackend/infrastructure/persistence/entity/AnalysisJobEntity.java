package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisJobStatus;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.jpa.StringListJsonConverter;

@Entity
@Table(name = "analysis_jobs")
public class AnalysisJobEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisJobStatus status;

    @Convert(converter = StringListJsonConverter.class)
    @Column(nullable = false, columnDefinition = "CLOB")
    private List<String> selectedRuleIds;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant completedAt;

    @Column(length = 2000)
    private String errorMessage;

    public static AnalysisJobEntity pending(String contractId, List<String> selectedRuleIds) {
        AnalysisJobEntity entity = new AnalysisJobEntity();
        entity.id = UUID.randomUUID().toString();
        entity.contractId = contractId;
        entity.selectedRuleIds = selectedRuleIds == null ? List.of() : selectedRuleIds;
        entity.status = AnalysisJobStatus.PENDING;
        entity.createdAt = Instant.now();
        return entity;
    }

    public String getId() {
        return id;
    }

    public String getContractId() {
        return contractId;
    }

    public AnalysisJobStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisJobStatus status) {
        this.status = status;
    }

    public List<String> getSelectedRuleIds() {
        return selectedRuleIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

