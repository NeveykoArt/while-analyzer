package ru.tlp.analyzer;

public record AnalysisResult(
        boolean success,
        String message
) {
}