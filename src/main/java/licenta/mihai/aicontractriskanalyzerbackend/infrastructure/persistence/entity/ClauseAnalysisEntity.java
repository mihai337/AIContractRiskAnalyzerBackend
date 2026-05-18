package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

@Entity
@Table(name = "clause_analysis")
@Getter
@Setter
@NoArgsConstructor
public class ClauseAnalysisEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "clause_id", nullable = false)
    private String clauseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
