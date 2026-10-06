package ru.tlp;

import javafx.scene.control.IndexRange;
import ru.tlp.analyzer.Diagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ErrorHighlightRanges {
    private ErrorHighlightRanges() {}

    static List<IndexRange> from(String text, List<Diagnostic> errors) {
        List<IndexRange> ranges = new ArrayList<>();
        for (Diagnostic error : errors) {
            if (error.position() < 0) {
                continue;
            }
            int start = Math.min(error.position(), text.length());
            int end = (int) Math.min(text.length(), (long) start + Math.max(0, error.length()));
            ranges.add(new IndexRange(start, end));
        }
        ranges.sort(Comparator.comparingInt(IndexRange::getStart));
        List<IndexRange> merged = new ArrayList<>();
        for (IndexRange range : ranges) {
            if (!merged.isEmpty() && range.getStart() <= merged.getLast().getEnd()) {
                IndexRange previous = merged.removeLast();
                merged.add(new IndexRange(previous.getStart(), Math.max(previous.getEnd(), range.getEnd())));
            } else {
                merged.add(range);
            }
        }
        return List.copyOf(merged);
    }
}
