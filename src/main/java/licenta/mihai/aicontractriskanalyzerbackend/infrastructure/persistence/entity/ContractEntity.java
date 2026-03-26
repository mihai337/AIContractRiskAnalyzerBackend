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
import java.util.UUID;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisStatus;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.jpa.ContractAnalysisResultConverter;

@Entity
@Table(name = "contracts")
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

    @Lob
    @Convert(converter = ContractAnalysisResultConverter.class)
    @Column(name = "analysis_json", columnDefinition = "CLOB")
    private ContractAnalysisResult analysis;

    public static ContractEntity pending(String fileName, String sourceUri, String mimeType, String base64Content) {
        ContractEntity entity = new ContractEntity();
        entity.id = UUID.randomUUID().toString();
        entity.fileName = fileName;
        entity.sourceUri = sourceUri;
        entity.mimeType = mimeType;
        entity.base64Content = base64Content;
        entity.uploadedAt = Instant.now();
        entity.status = AnalysisStatus.PENDING;
        return entity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public ContractAnalysisResult getAnalysis() {
        return analysis;
    }

    public void setAnalysis(ContractAnalysisResult analysis) {
        this.analysis = analysis;
    }
}


