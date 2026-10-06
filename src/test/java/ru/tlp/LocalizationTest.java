package ru.tlp;

import org.junit.jupiter.api.Test;
import ru.tlp.analyzer.Analyzer;

import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    @Test
    void formatsEveryErrorWithLocationInBothLanguages() {
        String code = "while (x) {\n x = ;\n y = ;\n}";
        var result = new Analyzer().analyze(code);
        String english = Localization.analysis(result, code, true);
        assertTrue(english.startsWith("Errors: 2"), english);
        assertTrue(english.contains("Line 2, column 5"), english);
        assertTrue(english.contains("Line 3, column 5"), english);
        assertFalse(english.matches("(?s).*[А-Яа-яЁё].*"), english);
        assertTrue(Localization.analysis(result, code, false).startsWith("Ошибок: 2"));
    }
    @Test
    void switchesInterfaceTextInBothDirections() {
        assertEquals("File", Localization.text("Файл", true));
        assertEquals("Файл", Localization.text("Файл", false));
        assertEquals("English", Localization.text("English", true));
    }

    @Test
    void translatesActualAnalysisResultsWithoutChangingRussianMessages() {
        Analyzer analyzer = new Analyzer();
        for (String code : new String[] {
                "while (x < 10) { x++; }",
                "while (x < 10) { x++ }",
                "while () { x++; }",
                "while (x) { x + 1; }",
                "while (x @ 10) { x++; }"
        }) {
            String russian = analyzer.analyze(code).message();
            String english = Localization.diagnostic(russian, true);
            assertFalse(english.matches("(?s).*[А-Яа-яЁё].*"), english);
            assertEquals(russian, Localization.diagnostic(russian, false));
        }
    }

    @Test
    void retainsUnexpectedCharacterAndPositionInTranslation() {
        assertEquals("Unexpected character '@' at position 9",
                Localization.diagnostic("Неожиданный символ '@' в позиции 9", true));
    }
}
