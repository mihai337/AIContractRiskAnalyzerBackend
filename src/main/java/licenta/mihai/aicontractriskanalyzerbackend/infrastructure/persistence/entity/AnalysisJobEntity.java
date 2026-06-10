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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import licenta.mihai.aicontractriskanalyzerbackend.models.AnalysisJobStatus;
import licenta.mihai.aicontractriskanalyzerbackend.utils.StringListJsonConverter;

@Entity
@Table(name = "analysis_jobs")
@Getter
@Setter
@NoArgsConstructor
public class AnalysisJobEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisJobStatus status;

    @Convert(converter = StringListJsonConverter.class)
    @Column(nullable = false, columnDefinition = "text")
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
}

