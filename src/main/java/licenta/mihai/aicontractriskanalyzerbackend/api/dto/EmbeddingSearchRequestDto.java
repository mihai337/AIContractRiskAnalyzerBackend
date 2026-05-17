package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record EmbeddingSearchRequestDto(
    @NotBlank String text,
    @Positive int limit
) {
}

