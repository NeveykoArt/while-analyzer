package ru.tlp.analyzer;

public record Diagnostic(String message, int position, int length) {
}
