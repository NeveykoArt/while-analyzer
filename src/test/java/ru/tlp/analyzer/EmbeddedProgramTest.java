package ru.tlp.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedProgramTest {
    private final Analyzer analyzer = new Analyzer();
    private static final String[] PROGRAMS = {
            "while (true) { break; }",
            "while (x < 10 && !done) { x += 1; continue; }",
            "while (true) while (y > 0) { y--; }",
            "while (готов) { счетчик++; while (x) continue; }",
            "while (x) x++;"
    };

    record Example(String name, String prefix, String program, String suffix) {
        @Override public String toString() {
            return name + ": " + prefix + " " + program + " " + suffix;
        }
    }

    static Stream<Example> tokenNoise() {
        String[] noise = { "a", "abc def ghi", "123 456", "true false", "break continue",
                "+ - * / %", "= += != && ||", ") } ;", "{ } } ;",
                "whileFlag breakValue continueName", "мусор 变量 _123", "a\n\tb\r\nc" };
        return Stream.of(noise).flatMap(junk -> Stream.of(PROGRAMS).flatMap(program -> Stream.of(
                new Example("Перед циклом", junk, program, ""),
                new Example("После цикла", "", program, junk),
                new Example("С обеих сторон", junk, program, junk))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokenNoise")
    void highlightsNoiseAsWholeFragmentsAndPreservesEmbeddedProgram(Example example) {
        assertTrue(analyzer.analyze(example.program()).success());
        String code = example.prefix() + " " + example.program() + " " + example.suffix();
        var result = analyzer.analyze(code);
        int expected = (example.prefix().isEmpty() ? 0 : 1) + (example.suffix().isEmpty() ? 0 : 1);
        assertEquals(expected, result.errors().size(), result.message());
        if (!example.prefix().isEmpty()) {
            assertEquals(0, result.errors().getFirst().position());
            assertEquals(example.prefix().length(), result.errors().getFirst().length());
        }
        if (!example.suffix().isEmpty()) {
            assertEquals(example.prefix().length() + example.program().length() + 2,
                    result.errors().getLast().position());
            assertEquals(example.suffix().length(), result.errors().getLast().length());
        }
    }

    static Stream<Example> characterNoise() {
        return Stream.of("@#$", "[],:?", "\"'\\~^", "@\n#\t$").flatMap(junk ->
                Stream.of(PROGRAMS).flatMap(program -> Stream.of(
                        new Example("Неизвестные символы перед циклом", junk, program, ""),
                        new Example("Неизвестные символы после цикла", "", program, junk),
                        new Example("Неизвестные символы с обеих сторон", junk, program, junk))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("characterNoise")
    void reportsEachUnknownCharacterAtItsExactPosition(Example example) {
        String code = example.prefix() + " " + example.program() + " " + example.suffix();
        int programStart = example.prefix().length() + 1;
        int programEnd = programStart + example.program().length();
        var errors = analyzer.analyze(code).errors();
        int index = 0;
        for (int position = 0; position < code.length(); position++) {
            if (position >= programStart && position < programEnd || Character.isWhitespace(code.charAt(position))) {
                continue;
            }
            assertTrue(index < errors.size(), code);
            assertEquals(position, errors.get(index).position(), code);
            assertEquals(1, errors.get(index).length(), code);
            assertTrue(errors.get(index).message().startsWith("Неожиданный символ"), code);
            index++;
        }
        assertEquals(index, errors.size(), code);
    }

    @Test
    void mixedLexicalAndSyntaxNoiseDoesNotCreateErrorsInsideValidProgram() {
        String prefix = "@ garbage # more $";
        String suffix = "? tail ~ end ^";
        for (String program : PROGRAMS) {
            String code = prefix + " " + program + " " + suffix;
            var errors = analyzer.analyze(code).errors();
            assertEquals(8, errors.size(), errors.toString()); // Six characters, two token fragments.
            int start = prefix.length() + 1;
            int end = start + program.length();
            for (var error : errors) {
                assertTrue(error.position() + error.length() <= start || error.position() >= end, code);
            }
        }
    }

    @Test
    void randomTokenNoiseOfDifferentLengthsPreservesTheWholeLoop() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Random random = new Random(20261007);
            String[] pieces = { "junk", "abc", "123", "true", "false", "break", "continue",
                    "=", "+", "*", "&&", "||", ";", ")", "}", "{" };
            for (int length : new int[] { 1, 2, 10, 50, 200, 1000 }) {
                for (int sample = 0; sample < 20; sample++) {
                    StringBuilder junk = new StringBuilder();
                    for (int i = 0; i < length; i++) {
                        if (i != 0) junk.append(i % 3 == 0 ? '\n' : ' ');
                        junk.append(pieces[random.nextInt(pieces.length)]);
                    }
                    for (String program : PROGRAMS) {
                        highlightsNoiseAsWholeFragmentsAndPreservesEmbeddedProgram(
                                new Example("Случайный фрагмент, seed=20261007", junk.toString(), program, junk.toString()));
                    }
                }
            }
        });
    }

    @Test
    void whitespaceAroundLoopIsNotAnError() {
        for (String whitespace : new String[] { "", " ", "\t", "\r\n", "\n\n\t", "\f" }) {
            for (String program : PROGRAMS) {
                assertTrue(analyzer.analyze(whitespace + program + whitespace).success());
            }
        }
    }
}
