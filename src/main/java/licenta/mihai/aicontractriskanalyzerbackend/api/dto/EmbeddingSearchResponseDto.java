package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import java.util.List;

public record EmbeddingSearchResponseDto(
    List<EmbeddingMatchDto> matches
) {
}

