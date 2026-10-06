package ru.tlp;

import javafx.scene.control.IndexRange;
import org.junit.jupiter.api.Test;
import ru.tlp.analyzer.Diagnostic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHighlightRangesTest {
    @Test
    void mergesOverlapsAndClampsRangesToText() {
        assertEquals(List.of(new IndexRange(1, 5)), ErrorHighlightRanges.from("abcde", List.of(
                new Diagnostic("one", 1, 3), new Diagnostic("two", 2, 100))));
    }

    @Test
    void preservesInsertionPointsAtEndOfFileAndBeforeNextStatement() {
        assertEquals(List.of(new IndexRange(7, 7)), ErrorHighlightRanges.from("x++;\n  ",
                List.of(new Diagnostic("missing token", 7, 0))));
        assertEquals(List.of(new IndexRange(2, 2)), ErrorHighlightRanges.from("x;\ny;",
                List.of(new Diagnostic("missing token", 2, 0))));
    }

    @Test
    void handlesEmptyTextAndUnknownPositions() {
        assertEquals(List.of(new IndexRange(0, 0)),
                ErrorHighlightRanges.from("", List.of(new Diagnostic("empty", 0, 0))));
        assertTrue(ErrorHighlightRanges.from("abc", List.of(new Diagnostic("unknown", -1, 1))).isEmpty());
    }
}
