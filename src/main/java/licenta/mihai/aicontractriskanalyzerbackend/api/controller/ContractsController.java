package licenta.mihai.aicontractriskanalyzerbackend.api.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.AnalysisJobDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.AnalyzeContractRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ContractRecordDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.EmbeddingMatchDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.EmbeddingSearchRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.EmbeddingSearchResponseDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ExtractTextRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ExtractTextResponseDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.UploadContractRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.mapper.ApiMapper;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.AnalysisJobService;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.ContractAnalysisService;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.ContractService;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.TextExtractionService;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.EmbeddingStoreService;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractsController {

    private final ContractService contractService;
    private final AnalysisJobService analysisJobService;
    private final ContractAnalysisService contractAnalysisService;
    private final TextExtractionService textExtractionService;
    private final ApiMapper apiMapper;
    private final EmbeddingStoreService embeddingStoreService;
    private final MlInferencePort mlInferencePort;


    @PostMapping("/extract-text")
    public ExtractTextResponseDto extractText(@Valid @RequestBody ExtractTextRequestDto request) {
        return apiMapper.toExtractTextResponseDto(
            textExtractionService.extract(request.fileName(), request.mimeType(), request.base64Content())
        );
    }

    @PostMapping("/upload")
    public ContractRecordDto upload(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UploadContractRequestDto request) {
        log.info("Received request to upload contract record");
        String ownerId = resolveOwnerId(jwt);
        ContractEntity entity = contractService.upload(
            request.fileName(),
            request.sourceUri(),
            request.mimeType(),
            request.base64Content(),
            ownerId
        );
        return apiMapper.toContractRecordDto(entity);
    }

    @GetMapping
    public List<ContractRecordDto> listContracts(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = resolveOwnerId(jwt);
        return contractService.list(ownerId).stream().map(apiMapper::toContractRecordDto).toList();
    }

    @GetMapping("/{contractId}")
    public ContractRecordDto getContract(@AuthenticationPrincipal Jwt jwt, @PathVariable String contractId) {
        String ownerId = resolveOwnerId(jwt);
        return apiMapper.toContractRecordDto(contractService.getOrThrow(contractId, ownerId));
    }

    @PostMapping("/{contractId}")
    public ContractRecordDto upsertContract(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String contractId,
        @Valid @RequestBody ContractRecordDto request
    ) {
        if (!contractId.equals(request.id())) {
            throw new IllegalArgumentException("Path contractId and payload id must match");
        }

        String ownerId = resolveOwnerId(jwt);
        ContractEntity existing = contractService.findById(contractId, ownerId).orElse(null);
        String mimeType = existing == null ? "application/pdf" : existing.getMimeType();
        String base64Content = existing == null ? "" : existing.getBase64Content();
        ContractEntity saved = contractService.upsert(apiMapper.toContractEntity(request, mimeType, base64Content), ownerId);
        return apiMapper.toContractRecordDto(saved);
    }

    @PostMapping("/{contractId}/analyze")
    public ContractRecordDto analyze(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String contractId,
        @Valid @RequestBody AnalyzeContractRequestDto request
    ) {
        String ownerId = resolveOwnerId(jwt);
        contractService.getOrThrow(contractId, ownerId);
        return apiMapper.toContractRecordDto(contractAnalysisService.analyze(contractId, request.selectedRuleIds()));
    }

    @PostMapping("/{contractId}/analysis-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisJobDto createAnalysisJob(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String contractId,
        @Valid @RequestBody AnalyzeContractRequestDto request
    ) {
        String ownerId = resolveOwnerId(jwt);
        return apiMapper.toAnalysisJobDto(analysisJobService.createJob(contractId, request.selectedRuleIds(), ownerId));
    }

    @GetMapping("/{contractId}/analysis-jobs/{jobId}")
    public AnalysisJobDto getAnalysisJob(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String contractId,
        @PathVariable String jobId
    ) {
        String ownerId = resolveOwnerId(jwt);
        return apiMapper.toAnalysisJobDto(analysisJobService.getJobOrThrow(contractId, jobId, ownerId));
    }

    @PostMapping("/retrieval")
    public EmbeddingSearchResponseDto retrieveSimilarClauses(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody EmbeddingSearchRequestDto request
    ) {
        resolveOwnerId(jwt);
        List<List<Double>> embeddings = mlInferencePort.embedTexts(List.of(request.text()));
        if (embeddings.isEmpty()) {
            return new EmbeddingSearchResponseDto(List.of());
        }
        List<EmbeddingMatch> matches = embeddingStoreService.findSimilarClauses(embeddings.get(0), request.limit());
        List<EmbeddingMatchDto> dtos = matches.stream()
            .map(match -> new EmbeddingMatchDto(
                match.clauseId(),
                match.contractId(),
                match.clauseType(),
                match.snippet(),
                match.distance()
            ))
            .toList();
        return new EmbeddingSearchResponseDto(dtos);
    }

    private String resolveOwnerId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Missing authentication token");
        }
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing user identity in token");
        }
        return userId;
    }
}
