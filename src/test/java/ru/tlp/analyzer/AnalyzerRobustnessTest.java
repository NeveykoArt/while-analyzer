package ru.tlp.analyzer;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzerRobustnessTest {
    private final Analyzer analyzer = new Analyzer();

    @Test
    void everyTruncatedPrefixTerminatesAndReturnsSourceBoundedDiagnostics() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            String[] programs = {
                    "while (x < 10 && !done) { x += 1; continue; }",
                    "while (true) while ((x + 2) > y) { while (z) z--; break; }",
                    "while (готов) { счетчик++; }"
            };
            for (String program : programs) {
                for (int end = 0; end <= program.length(); end++) {
                    checkBounds(program.substring(0, end));
                }
            }
        });
    }

    @Test
    void deletingEachTokenDoesNotCrashOrProduceInvalidHighlightRanges() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            String code = "while (x < 10) { while (y) { y -= 1; } x++; continue; }";
            var tokens = new ru.tlp.lexer.Lexer(code).tokenize();
            for (var token : tokens) {
                int start = token.getPosition();
                int end = start + token.getValue().length();
                checkBounds(code.substring(0, start) + code.substring(end));
            }
        });
    }

    @Test
    void seededMalformedInputsTerminateAndHaveOrderedDiagnostics() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Random random = new Random(20261006);
            String[] pieces = { "while", "break", "continue", "true", "false", "x", "1",
                    "(", ")", "{", "}", ";", "=", "+", "*", "&&", "||", "!", "@", "#" };
            for (int example = 0; example < 2000; example++) {
                StringBuilder code = new StringBuilder();
                int length = random.nextInt(60);
                for (int i = 0; i < length; i++) {
                    code.append(pieces[random.nextInt(pieces.length)]).append(' ');
                }
                checkBounds(code.toString());
            }
        });
    }

    @Test
    void repeatedAnalysisDoesNotRetainErrorsFromPreviousDocument() {
        assertFalse(analyzer.analyze("while (break) {}").success());
        assertTrue(analyzer.analyze("while (true) {}").success());
        assertFalse(analyzer.analyze("").success());
        assertTrue(analyzer.analyze("while (x) continue;").success());
    }

    @Test
    void handlesLargeBlocksAndNestedLoops() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            assertTrue(analyzer.analyze("while (x) { " + "x++; ".repeat(3000) + "}").success());
            assertEquals(500, analyzer.analyze("while (x) { " + "x = ; ".repeat(500) + "}").errors().size());
            assertTrue(analyzer.analyze("while (x) { ".repeat(100) + "break;" + "}".repeat(100)).success());
        });
    }

    private void checkBounds(String code) {
        var result = analyzer.analyze(code);
        assertEquals(result.errors().isEmpty(), result.success(), code);
        int previous = -1;
        for (var error : result.errors()) {
            assertTrue(error.position() >= previous, code);
            assertTrue(error.position() >= 0 && error.position() <= code.length(), code);
            assertTrue(error.length() >= 0 && error.position() + error.length() <= code.length(), code);
            assertFalse(error.message().isBlank(), code);
            previous = error.position();
        }
    }
}
