package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import licenta.mihai.aicontractriskanalyzerbackend.models.AnalysisStatus;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.utils.ContractAnalysisResultConverter;
import licenta.mihai.aicontractriskanalyzerbackend.utils.StringListJsonConverter;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
public class ContractEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String sourceUri;

    @Column(nullable = false)
    private String mimeType;

    @Lob
    @Column(nullable = false)
    private String base64Content;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Convert(converter = ContractAnalysisResultConverter.class)
    @Column(name = "analysis_json", columnDefinition = "text")
    private ContractAnalysisResult analysis;

    @Column(name = "ml_engine")
    private String mlEngine;

    @Column(name = "ml_analysis_success")
    private Boolean mlAnalysisSuccess;

    @Column(name = "ml_analyzed_at")
    private Instant mlAnalyzedAt;

    @Column(nullable = false)
    private String ownerId;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "selected_rule_ids", columnDefinition = "text")
    private List<String> selectedRuleIds;

    public static ContractEntity pending(String fileName, String sourceUri, String mimeType, String base64Content, String ownerId) {
        ContractEntity entity = new ContractEntity();
        entity.id = UUID.randomUUID().toString();
        entity.fileName = fileName;
        entity.sourceUri = sourceUri;
        entity.mimeType = mimeType;
        entity.base64Content = base64Content;
        entity.uploadedAt = Instant.now();
        entity.status = AnalysisStatus.PENDING;
        entity.ownerId = ownerId;
        entity.selectedRuleIds = List.of();
        return entity;
    }
}
