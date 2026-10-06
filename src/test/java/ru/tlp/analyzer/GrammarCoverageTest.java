package ru.tlp.analyzer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Expectations are hand-selected from the documented grammar, not parser output. */
class GrammarCoverageTest {
    record Example(String name, boolean valid, String code) {
        @Override public String toString() { return name + ": " + code; }
    }

    static Stream<Example> examples() throws IOException {
        try (var input = GrammarCoverageTest.class.getResourceAsStream("/grammar-cases.tsv")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(line -> {
                        String[] columns = line.split("\t", 3);
                        return new Example(columns[0], Boolean.parseBoolean(columns[1]), columns[2]);
                    }).toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void checksAnalyzerSyntaxAndDiagnosticRanges(Example example) {
        var result = new Analyzer().analyze(example.code());
        if (example.valid()) {
            assertTrue(result.success(), result.message());
            assertTrue(result.errors().isEmpty());
            assertEquals(-1, result.errorPosition());
        } else {
            assertFalse(result.success(), example.toString());
            assertFalse(result.errors().isEmpty());
        }
        int previous = -1;
        for (var error : result.errors()) {
            assertTrue(error.position() >= previous, result.message());
            assertTrue(error.position() >= 0 && error.position() <= example.code().length());
            assertTrue(error.length() >= 0 && error.position() + error.length() <= example.code().length());
            previous = error.position();
        }
    }
}
