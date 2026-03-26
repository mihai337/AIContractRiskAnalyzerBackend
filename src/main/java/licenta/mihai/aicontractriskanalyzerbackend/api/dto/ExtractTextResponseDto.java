package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

public record ExtractTextResponseDto(
    String text,
    String extractionEngine,
    boolean containsScannedPages
) {
}

