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
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

@Entity
@Table(name = "custom_rules")
@Getter
@Setter
@NoArgsConstructor
public class CustomRuleEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ClauseType requiredClause;

    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel severity;

    @Column(nullable = false)
    private boolean enabled;
}

