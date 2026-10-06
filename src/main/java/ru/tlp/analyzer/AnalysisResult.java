package ru.tlp.analyzer;

import java.util.List;

public record AnalysisResult(
        boolean success,
        String message,
        int errorPosition,
        int errorLength,
        List<Diagnostic> errors
) {
    public AnalysisResult {
        errors = List.copyOf(errors);
    }
}
