package ru.tlp.analyzer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzerTest {

    private final Analyzer analyzer = new Analyzer();

    @Test
    void acceptsValidCode() {
        AnalysisResult result = analyzer.analyze("""
                while (x < 10) {
                    x++;
                }
                """);

        assertTrue(result.success());
        assertEquals("Синтаксис корректен", result.message());
    }

    @Test
    void rejectsSyntaxError() {
        AnalysisResult result = analyzer.analyze("""
                while (x < 10) {
                    x++
                }
                """);

        assertFalse(result.success());
        assertNotNull(result.message());
    }

    @Test
    void rejectsLexicalError() {
        AnalysisResult result = analyzer.analyze("""
                while (x @ 10) {
                    x++;
                }
                """);

        assertFalse(result.success());
        assertNotNull(result.message());
    }
}