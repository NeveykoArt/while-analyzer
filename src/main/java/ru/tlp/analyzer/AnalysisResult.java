package ru.tlp.analyzer;

public record AnalysisResult(
        boolean success,
        String message,
        int errorPosition,
        int errorLength
) {
}