package licenta.mihai.aicontractriskanalyzerbackend.models;

public record ExtractedText(
    String text,
    String extractionEngine,
    boolean containsScannedPages
) {
}

