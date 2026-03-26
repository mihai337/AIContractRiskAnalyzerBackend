package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ExtractTextRequestDto(
    @NotBlank String fileName,
    @NotBlank String mimeType,
    @NotBlank String base64Content
) {
}

