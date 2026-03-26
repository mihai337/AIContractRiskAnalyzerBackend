package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadContractRequestDto(
    @NotBlank String fileName,
    @NotBlank String sourceUri,
    @NotBlank String mimeType,
    @NotBlank String base64Content
) {
}

