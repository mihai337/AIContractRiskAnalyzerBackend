package licenta.mihai.aicontractriskanalyzerbackend.api.controller;

import jakarta.validation.Valid;
import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.AnalysisJobDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.AnalyzeContractRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ContractRecordDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ExtractTextRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.ExtractTextResponseDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.UploadContractRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.mapper.ApiMapper;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.AnalysisJobService;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.ContractAnalysisService;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.ContractService;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.TextExtractionService;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/contracts")
public class ContractsController {

    private final ContractService contractService;
    private final AnalysisJobService analysisJobService;
    private final ContractAnalysisService contractAnalysisService;
    private final TextExtractionService textExtractionService;
    private final ApiMapper apiMapper;

    public ContractsController(
        ContractService contractService,
        AnalysisJobService analysisJobService,
        ContractAnalysisService contractAnalysisService,
        TextExtractionService textExtractionService,
        ApiMapper apiMapper
    ) {
        this.contractService = contractService;
        this.analysisJobService = analysisJobService;
        this.contractAnalysisService = contractAnalysisService;
        this.textExtractionService = textExtractionService;
        this.apiMapper = apiMapper;
    }

    @PostMapping("/extract-text")
    public ExtractTextResponseDto extractText(@Valid @RequestBody ExtractTextRequestDto request) {
        return apiMapper.toExtractTextResponseDto(
            textExtractionService.extract(request.fileName(), request.mimeType(), request.base64Content())
        );
    }

    @PostMapping("/upload")
    public ContractRecordDto upload(@Valid @RequestBody UploadContractRequestDto request) {
        ContractEntity entity = contractService.upload(
            request.fileName(),
            request.sourceUri(),
            request.mimeType(),
            request.base64Content()
        );
        return apiMapper.toContractRecordDto(entity);
    }

    @GetMapping
    public List<ContractRecordDto> listContracts() {
        return contractService.list().stream().map(apiMapper::toContractRecordDto).toList();
    }

    @GetMapping("/{contractId}")
    public ContractRecordDto getContract(@PathVariable String contractId) {
        return apiMapper.toContractRecordDto(contractService.getOrThrow(contractId));
    }

    @PostMapping("/{contractId}")
    public ContractRecordDto upsertContract(@PathVariable String contractId, @Valid @RequestBody ContractRecordDto request) {
        if (!contractId.equals(request.id())) {
            throw new IllegalArgumentException("Path contractId and payload id must match");
        }

        ContractEntity existing = contractService.findById(contractId).orElse(null);
        String mimeType = existing == null ? "application/pdf" : existing.getMimeType();
        String base64Content = existing == null ? "" : existing.getBase64Content();
        ContractEntity saved = contractService.upsert(apiMapper.toContractEntity(request, mimeType, base64Content));
        return apiMapper.toContractRecordDto(saved);
    }

    @PostMapping("/{contractId}/analyze")
    public ContractRecordDto analyze(@PathVariable String contractId, @Valid @RequestBody AnalyzeContractRequestDto request) {
        return apiMapper.toContractRecordDto(contractAnalysisService.analyze(contractId, request.selectedRuleIds()));
    }

    @PostMapping("/{contractId}/analysis-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisJobDto createAnalysisJob(@PathVariable String contractId, @Valid @RequestBody AnalyzeContractRequestDto request) {
        return apiMapper.toAnalysisJobDto(analysisJobService.createJob(contractId, request.selectedRuleIds()));
    }

    @GetMapping("/{contractId}/analysis-jobs/{jobId}")
    public AnalysisJobDto getAnalysisJob(@PathVariable String contractId, @PathVariable String jobId) {
        return apiMapper.toAnalysisJobDto(analysisJobService.getJobOrThrow(contractId, jobId));
    }
}


