package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

@Entity
@Table(name = "detected_issues")
@Getter
@Setter
@NoArgsConstructor
public class DetectedIssueEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "clause_analysis_id", nullable = false)
    private String clauseAnalysisId;

    @Column(name = "issue_type", nullable = false)
    private String issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel severity;

    @Column(nullable = false)
    private String explanation;

    @Column(name = "highlighted_text", nullable = false)
    private String highlightedText;
}

