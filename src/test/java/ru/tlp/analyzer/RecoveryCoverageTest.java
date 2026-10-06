package ru.tlp.analyzer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryCoverageTest {
    record Example(String name, String code, String fragment, int count) {
        @Override public String toString() { return name + ": " + code; }
    }

    static Stream<Example> recoveries() {
        Stream.Builder<Example> cases = Stream.builder();
        for (String keyword : new String[] { "break", "continue" }) {
            for (String condition : new String[] { keyword, "(" + keyword + ")",
                    "x < " + keyword, "x && " + keyword, "x || " + keyword,
                    "!" + keyword, "x + " + keyword, "x * " + keyword }) {
                String prefix = "while (true) while (" + condition + ") { ";
                cases.add(new Example("Недопустимое слово в условии", prefix + "b; }", keyword, 1));
                cases.add(new Example("Сохранение ошибки в теле", prefix + "b = ; }", keyword, 2));
            }
        }
        for (String statement : new String[] { "x = ;", "x += ;", "x = y + ;",
                "x = y * ;", "++;", "--;", "x++ y++;", "break continue;" }) {
            cases.add(new Example("Независимые ошибки", "while (x) { " + statement + " z = ; }", "", 2));
        }
        return cases.build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("recoveries")
    void reportsRootErrorsAndRetainsLaterErrors(Example example) {
        var result = new Analyzer().analyze(example.code());
        assertEquals(example.count(), result.errors().size(), result.message());
        if (!example.fragment().isEmpty()) {
            assertEquals(example.code().indexOf(example.fragment()), result.errorPosition());
            assertEquals(example.fragment().length(), result.errorLength());
        }
    }
}
