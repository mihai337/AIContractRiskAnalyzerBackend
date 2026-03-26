package licenta.mihai.aicontractriskanalyzerbackend.domain.model;

public record ExtractedText(
    String text,
    String extractionEngine,
    boolean containsScannedPages
) {
}

