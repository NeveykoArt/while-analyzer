package ru.tlp;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.tlp.analyzer.Analyzer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHighlightTextAreaTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void mainWindowCommandsAndLanguageBindingsSurviveRefactoring() throws Exception {
        onFxThread(() -> {
            Platform.setImplicitExit(false);
            Stage stage = new Stage();
            try {
                new Main().start(stage);
                BorderPane root = (BorderPane) ((Pane) stage.getScene().getRoot()).getChildren().getFirst();
                VBox top = (VBox) root.getTop();
                MenuBar menus = (MenuBar) top.getChildren().get(0);
                ToolBar toolbar = (ToolBar) top.getChildren().get(1);
                assertEquals(java.util.List.of("Файл", "Правка", "", "Пуск", "Справка", "Локализация", "Вид"),
                        menus.getMenus().stream().map(Menu::getText).toList());
                assertTrue(menus.getMenus().get(2).getItems().isEmpty());
                assertEquals("Текст", ((Button) menus.getMenus().get(2).getGraphic()).getText());
                assertEquals(11, toolbar.getItems().stream().filter(Button.class::isInstance).count());

                SplitPane panes = (SplitPane) root.getCenter();
                HBox editorBox = (HBox) panes.getItems().get(0);
                TextArea numbers = (TextArea) editorBox.getChildren().get(0);
                ErrorHighlightTextArea editor = (ErrorHighlightTextArea) editorBox.getChildren().get(1);
                TextArea output = (TextArea) ((VBox) panes.getItems().get(1)).getChildren().get(1);
                editor.setText("while (x) {\n x = ;\n}");
                assertEquals("1\n2\n3\n", numbers.getText());
                menus.getMenus().get(3).getItems().getFirst().fire();
                assertTrue(output.getText().startsWith("Ошибок: 1"));
                menus.getMenus().get(5).getItems().get(1).fire();
                assertEquals("File", menus.getMenus().getFirst().getText());
                assertEquals("Text", ((Button) menus.getMenus().get(2).getGraphic()).getText());
                assertEquals("New", ((Button) toolbar.getItems().getFirst()).getTooltip().getText());
                assertTrue(output.getText().startsWith("Errors: 1"));
                assertEquals("while (x) {\n x = ;\n}", editor.getText());
                Menu font = (Menu) menus.getMenus().get(6).getItems().getFirst();
                font.getItems().getFirst().fire();
                assertTrue(editor.getStyle().contains("16px"));
                font.getItems().get(1).fire();
                assertTrue(editor.getStyle().contains("15px"));

                stage.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                        false, false, false, false));
                Stage help = Window.getWindows().stream().filter(Stage.class::isInstance).map(Stage.class::cast)
                        .filter(window -> window.getOwner() == stage).findFirst().orElseThrow();
                TextArea guide = (TextArea) ((BorderPane) help.getScene().getRoot()).getCenter();
                assertTrue(guide.getText().startsWith("USER GUIDE"));
                help.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                        false, false, false, false));
                assertFalse(help.isShowing());

                ((Button) toolbar.getItems().getFirst()).fire();
                assertEquals("", editor.getText());
                assertEquals("", output.getText());
                assertTrue(stage.getTitle().endsWith("New document"));
                menus.getMenus().get(5).getItems().getFirst().fire();
                assertTrue(stage.getTitle().endsWith("Новый документ"));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void keepsAllErrorsVisibleIndependentlyOfSelectionAndClearsAfterEditing() throws Exception {
        onFxThread(() -> {
            ErrorHighlightTextArea editor = new ErrorHighlightTextArea();
            editor.setText("while (x) {\n x = ;\n y = ;\n}");
            StackPane root = new StackPane(editor);
            new Scene(root, 320, 160);
            root.applyCss();
            root.layout();
            editor.highlightErrors(new Analyzer().analyze(editor.getText()).errors());
            root.layout();
            assertEquals(2, editor.lookupAll(".analysis-error").size());
            for (var node : editor.lookupAll(".analysis-error")) {
                Path path = (Path) node;
                assertFalse(path.getElements().isEmpty());
                assertTrue(path.getBoundsInLocal().getWidth() > 0);
                assertTrue(path.isMouseTransparent());
            }
            editor.selectRange(0, 5);
            editor.deselect();
            root.layout();
            assertEquals(2, editor.lookupAll(".analysis-error").size());

            double oldHeight = editor.lookup(".analysis-error").getBoundsInLocal().getHeight();
            editor.setStyle("-fx-font-size: 30px;");
            root.applyCss();
            root.layout();
            assertEquals(2, editor.lookupAll(".analysis-error").size());
            assertTrue(editor.lookup(".analysis-error").getBoundsInLocal().getHeight() > oldHeight);

            editor.appendText(" ");
            root.layout();
            assertTrue(editor.lookupAll(".analysis-error").isEmpty());
        });
    }

    @Test
    void clearsHighlightsOnSuccessfulReanalysisAndHandlesEndOfFile() throws Exception {
        onFxThread(() -> {
            ErrorHighlightTextArea editor = new ErrorHighlightTextArea();
            editor.setText("while (x) { x++;");
            StackPane root = new StackPane(editor);
            new Scene(root, 320, 160);
            root.applyCss();
            root.layout();
            editor.highlightErrors(new Analyzer().analyze(editor.getText()).errors());
            root.layout();
            assertEquals(1, editor.lookupAll(".analysis-error").size());
            editor.highlightErrors(java.util.List.of());
            root.layout();
            assertTrue(editor.lookupAll(".analysis-error").isEmpty());
        });
    }

    private static void onFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    void distinguishesDiscardedFragmentsFromInsertionMarkers() throws Exception {
        onFxThread(() -> {
            ErrorHighlightTextArea editor = new ErrorHighlightTextArea();
            String code = "while (x) { x = = = 5; y = ; }";
            editor.setText(code);
            StackPane root = new StackPane(editor);
            new Scene(root, 500, 160);
            root.applyCss();
            root.layout();
            var result = new Analyzer().analyze(code);
            assertEquals(3, result.errors().getFirst().length());
            assertEquals(0, result.errors().getLast().length());
            editor.highlightErrors(result.errors());
            root.layout();
            assertEquals(2, editor.lookupAll(".analysis-error").size());
            assertEquals(1, editor.lookupAll(".analysis-error-insertion").size());
            var marker = editor.lookup(".analysis-error-insertion");
            assertEquals(3, ((Path) marker).getStrokeWidth());
            assertTrue(marker.getBoundsInLocal().getWidth() <= 4,
                    "Insertion marker must not cover the next valid token");
            for (var node : editor.lookupAll(".analysis-error")) {
                if (node != marker) {
                    assertTrue(node.getBoundsInLocal().getWidth() > marker.getBoundsInLocal().getWidth());
                }
            }
            assertEquals(code, editor.getText());
        });
    }

    @Test
    void followsScrollingAndMarksEmptyInput() throws Exception {
        onFxThread(() -> {
            ErrorHighlightTextArea editor = new ErrorHighlightTextArea();
            editor.setText("while (x) {\n" + "x++;\n".repeat(40) + "y = ;\n}");
            StackPane root = new StackPane(editor);
            new Scene(root, 320, 160);
            root.applyCss();
            root.layout();
            editor.highlightErrors(new Analyzer().analyze(editor.getText()).errors());
            root.layout();
            var highlight = editor.lookup(".analysis-error");
            double oldY = highlight.localToScene(highlight.getBoundsInLocal()).getMinY();
            editor.setScrollTop(200);
            root.layout();
            double newY = highlight.localToScene(highlight.getBoundsInLocal()).getMinY();
            assertTrue(newY < oldY, "Highlight must scroll with the text");

            editor.clear();
            editor.highlightErrors(new Analyzer().analyze("").errors());
            root.layout();
            assertEquals(1, editor.lookupAll(".analysis-error").size());
            assertTrue(editor.lookup(".analysis-error").getBoundsInLocal().getHeight() > 0);
        });
    }
}
