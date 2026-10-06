package ru.tlp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelpContentTest {
    @Test
    void loadsUserGuideInBothLanguages() {
        String russian = HelpContent.text(false);
        String english = HelpContent.text(true);
        assertFalse(russian.isBlank());
        assertFalse(english.isBlank());
        assertNotEquals(russian, english);
        assertTrue(russian.contains("Файл"));
        assertTrue(english.contains("File"));
        assertTrue(russian.contains("report.pdf"));
        assertTrue(english.contains("report.pdf"));
        assertFalse(russian.contains("@@"));
        assertFalse(english.contains("@@"));
    }
}