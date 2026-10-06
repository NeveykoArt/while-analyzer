package ru.tlp;

import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import ru.tlp.analyzer.Diagnostic;

import java.util.ArrayList;
import java.util.List;

final class ErrorHighlightTextArea extends TextArea {
    private List<IndexRange> errorRanges = List.of();

    ErrorHighlightTextArea() {
        textProperty().addListener(observable -> highlightErrors(List.of()));
    }

    void highlightErrors(List<Diagnostic> errors) {
        errorRanges = ErrorHighlightRanges.from(getText(), errors);
        if (getSkin() instanceof ErrorSkin skin) {
            skin.refresh();
        }
        requestLayout();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ErrorSkin(this);
    }

    private static final class ErrorSkin extends TextAreaSkin {
        private final ErrorHighlightTextArea editor;
        private final List<Node> highlights = new ArrayList<>();
        private boolean dirty = true;

        ErrorSkin(ErrorHighlightTextArea editor) {
            super(editor);
            this.editor = editor;
            registerChangeListener(editor.fontProperty(), observable -> refresh());
            registerChangeListener(editor.widthProperty(), observable -> refresh());
            registerChangeListener(editor.wrapTextProperty(), observable -> refresh());
        }

        void refresh() {
            removeHighlight(highlights);
            highlights.clear();
            dirty = true;
            editor.requestLayout();
        }

        @Override
        protected void layoutChildren(double x, double y, double width, double height) {
            super.layoutChildren(x, y, width, height);
            if (!dirty) {
                return;
            }
            getChildren().stream().filter(javafx.scene.Parent.class::isInstance)
                    .map(javafx.scene.Parent.class::cast).forEach(javafx.scene.Parent::layout);
            dirty = false;
            for (IndexRange range : editor.errorRanges) {
                Path highlight = range.getLength() == 0
                        ? insertionMarker(range.getStart())
                        : new Path(getRangeShape(range.getStart(), range.getEnd()));
                if (range.getLength() == 0) {
                    highlight.getStyleClass().add("analysis-error-insertion");
                }
                addErrorHighlight(highlight, range.getStart());
            }
        }

        private Path insertionMarker(int offset) {
            Text text = editor.lookupAll(".text").stream()
                    .filter(Text.class::isInstance).map(Text.class::cast)
                    .filter(node -> node.getText().equals(editor.getText()))
                    .findFirst().orElseThrow();
            return new Path(text.caretShape(offset, true));
        }

        private void addErrorHighlight(Path highlight, int start) {
            highlight.getStyleClass().add("analysis-error");
            highlight.setFill(Color.rgb(231, 76, 60, 0.30));
            highlight.setStroke(Color.rgb(192, 32, 32));
            highlight.setStrokeWidth(highlight.getStyleClass().contains("analysis-error-insertion") ? 3 : 1);
            highlight.setManaged(false);
            highlight.setMouseTransparent(true);
            addHighlight(List.of(highlight), start);
            highlights.add(highlight);
        }

        @Override
        public void dispose() {
            removeHighlight(highlights);
            highlights.clear();
            super.dispose();
        }
    }
}
