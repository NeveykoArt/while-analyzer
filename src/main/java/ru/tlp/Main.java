package ru.tlp;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.tlp.analyzer.Analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Main extends Application {
    private static final int MIN_TEXT_FONT_SIZE = 8;
    private static final int MAX_TEXT_FONT_SIZE = 40;
    private static final double MIN_UI_SCALE = 0.7;
    private static final double MAX_UI_SCALE = 1.8;
    private static final double UI_SCALE_STEP = 0.1;
    private static final double BASE_POPUP_FONT_SIZE = 13;

    private final Analyzer analyzer = new Analyzer();
    private final BooleanProperty english = new SimpleBooleanProperty(false);
    private final Scale scaleTransform = new Scale(1, 1, 0, 0);
    private final List<Tooltip> tooltips = new ArrayList<>();
    private ErrorHighlightTextArea codeArea;
    private TextArea outputArea;
    private TextArea lineNumbers;
    private Stage stage;
    private Stage helpStage;
    private BorderPane root;
    private MenuBar menuBar;
    private File currentFile;
    private boolean newDocument;
    private int textFontSize = 15;
    private double uiScale = 1.0;

    private record Command(String title, String iconFile, Runnable action) {}

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        root = new BorderPane();
        root.setTop(createTop());
        root.setCenter(createEditor());
        root.setManaged(false);
        root.getTransforms().add(scaleTransform);
        Scene scene = new Scene(new Pane(root), 1000, 700);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F1) {
                showHelp();
                event.consume();
            }
        });
        scene.widthProperty().addListener(observable -> updateScaledLayout(scene));
        scene.heightProperty().addListener(observable -> updateScaledLayout(scene));
        scene.addEventFilter(ScrollEvent.SCROLL, event -> handleZoom(event, scene));
        english.addListener(observable -> updateTitle());
        updateTitle();
        stage.setScene(scene);
        stage.show();
        updateScaledLayout(scene);
        menuBar.getMenus().forEach(this::updateMenuItemsScale);
        tooltips.forEach(this::updateTooltipScale);
    }

    private VBox createTop() {
        Command create = new Command("Создать", "create.png", this::newFile);
        Command open = new Command("Открыть", "open.png", this::openFile);
        Command save = new Command("Сохранить", "save.png", this::saveFile);
        Command undo = new Command("Отменить", "undo.png", () -> codeArea.undo());
        Command redo = new Command("Повторить", "redo.png", () -> codeArea.redo());
        Command cut = new Command("Вырезать", "cut.png", () -> codeArea.cut());
        Command copy = new Command("Копировать", "copy.png", () -> codeArea.copy());
        Command paste = new Command("Вставить", "paste.png", () -> codeArea.paste());
        Command analyze = new Command("Анализ", "analyze.png", this::analyze);
        Command help = new Command("Вызов справки", "help.png", this::showHelp);
        Command about = new Command("О программе", "about.png", this::showAbout);

        Button report = new Button();
        report.setFocusTraversable(false);
        localize(report.textProperty(), "Текст");
        report.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        report.setOnAction(event -> openReport());
        Menu textMenu = new Menu();
        textMenu.setGraphic(report);

        ToggleGroup languages = new ToggleGroup();
        menuBar = new MenuBar(
                menu("Файл", item(create), item(open), item(save), new SeparatorMenuItem(),
                        item("Выход", () -> stage.close())),
                menu("Правка", item(undo), item(redo), new SeparatorMenuItem(), item(cut), item(copy), item(paste)),
                textMenu,
                menu("Пуск", item(analyze)),
                menu("Справка", item(help), new SeparatorMenuItem(), item(about)),
                menu("Локализация", languageItem("Русская", false, languages),
                        languageItem("English", true, languages))
        );
        ToolBar toolbar = new ToolBar(
                button(create), button(open), button(save), new Separator(),
                button(undo), button(redo), new Separator(),
                button(cut), button(copy), button(paste), new Separator(),
                button(analyze, "Пуск"), button(help, "Справка"), button(about), new Separator(),
                fontSizeButton("font-decrease.png", "Уменьшить шрифт", -2),
                fontSizeButton("font-increase.png", "Увеличить шрифт", 2));
        return new VBox(menuBar, toolbar);
    }

    private Menu menu(String title, MenuItem... items) {
        Menu menu = new Menu();
        localize(menu.textProperty(), title);
        menu.getItems().addAll(items);
        return menu;
    }

    private MenuItem item(Command command) {
        return item(command.title(), command.action());
    }

    private MenuItem item(String title, Runnable action) {
        MenuItem item = new MenuItem();
        localize(item.textProperty(), title);
        item.setOnAction(event -> action.run());
        return item;
    }

    private RadioMenuItem languageItem(String title, boolean useEnglish, ToggleGroup group) {
        RadioMenuItem item = new RadioMenuItem(title);
        item.setToggleGroup(group);
        item.setSelected(english.get() == useEnglish);
        item.setOnAction(event -> english.set(useEnglish));
        return item;
    }

    private Button button(Command command) {
        return button(command, command.title());
    }

    private Button button(Command command, String tooltipTitle) {
        Button button = toolbarButton(command.title(), tooltipTitle, command.action());
        button.setGraphic(CommandIcons.createView(command.iconFile()));
        return button;
    }

    private Button fontSizeButton(String iconFile, String title, int delta) {
        return button(new Command(title, iconFile, () -> changeFontSize(delta)));
    }

    private Button toolbarButton(String title, String tooltipTitle, Runnable action) {
        Button button = new Button();
        // Keep clicks from taking keyboard focus and leaving the focus highlight visible.
        button.setFocusTraversable(false);
        button.accessibleTextProperty().bind(Bindings.createStringBinding(
                () -> text(title), english));
        button.setOnAction(event -> action.run());
        button.setPrefSize(42, 34);
        Tooltip tooltip = new Tooltip();
        localize(tooltip.textProperty(), tooltipTitle);
        button.setTooltip(tooltip);
        tooltips.add(tooltip);
        updateTooltipScale(tooltip);
        return button;
    }

    private void handleZoom(ScrollEvent event, Scene scene) {
        if ((!event.isControlDown() && !event.isMetaDown()) || event.getDeltaY() == 0) {
            return;
        }
        uiScale = Math.max(MIN_UI_SCALE, Math.min(MAX_UI_SCALE,
                uiScale + (event.getDeltaY() > 0 ? UI_SCALE_STEP : -UI_SCALE_STEP)));
        scaleTransform.setX(uiScale);
        scaleTransform.setY(uiScale);
        menuBar.getMenus().forEach(this::updateMenuItemsScale);
        tooltips.forEach(this::updateTooltipScale);
        updateScaledLayout(scene);
        event.consume();
    }

    private void updateScaledLayout(Scene scene) {
        root.resizeRelocate(0, 0, scene.getWidth() / uiScale, scene.getHeight() / uiScale);
    }

    private void updateMenuItemsScale(Menu menu) {
        for (MenuItem item : menu.getItems()) {
            item.setStyle(item instanceof SeparatorMenuItem
                    ? "-fx-padding: " + (3 * uiScale) + "px 0px;"
                    : "-fx-font-size: " + (BASE_POPUP_FONT_SIZE * uiScale) + "px;"
                    + "-fx-padding: " + (5 * uiScale) + "px " + (10 * uiScale) + "px;");
            if (item instanceof Menu subMenu) {
                updateMenuItemsScale(subMenu);
            }
        }
    }

    private void updateTooltipScale(Tooltip tooltip) {
        tooltip.setStyle("-fx-font-size: " + (BASE_POPUP_FONT_SIZE * uiScale) + "px;"
                + "-fx-padding: " + (6 * uiScale) + "px;");
    }

    private SplitPane createEditor() {
        codeArea = new ErrorHighlightTextArea();
        codeArea.setWrapText(false);
        lineNumbers = new TextArea("1");
        lineNumbers.setEditable(false);
        lineNumbers.setFocusTraversable(false);
        codeArea.textProperty().addListener(observable -> updateLineNumbers());
        lineNumbers.scrollTopProperty().bindBidirectional(codeArea.scrollTopProperty());
        HBox editor = new HBox(lineNumbers, codeArea);
        HBox.setHgrow(codeArea, Priority.ALWAYS);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle(fontStyle(14));
        Label outputLabel = new Label();
        localize(outputLabel.textProperty(), "Результат анализа");
        outputLabel.setPadding(new Insets(6));
        VBox output = new VBox(outputLabel, outputArea);
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(editor, output);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.68);
        updateTextFontSize();
        return splitPane;
    }

    private void updateLineNumbers() {
        int lines = codeArea.getText().split("\\n", -1).length;
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            numbers.append(i).append(System.lineSeparator());
        }
        lineNumbers.setText(numbers.toString());
    }

    private void analyze() {
        String code = codeArea.getText();
        var result = analyzer.analyze(code);
        setOutput(() -> Localization.analysis(result, code, english.get()));
        codeArea.highlightErrors(result.errors());
        if (result.success()) {
            codeArea.deselect();
        } else if (result.errorPosition() >= codeArea.getLength()) {
            codeArea.positionCaret(codeArea.getLength());
        } else if (result.errorPosition() >= 0) {
            codeArea.requestFocus();
            codeArea.selectRange(result.errorPosition(),
                    Math.min(result.errorPosition() + result.errorLength(), codeArea.getLength()));
        }
    }

    private void newFile() {
        codeArea.clear();
        setOutput(() -> "");
        currentFile = null;
        newDocument = true;
        updateTitle();
    }

    private FileChooser fileChooser(String title, String... extensions) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(text(title));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(text("Текстовые файлы"), extensions));
        return chooser;
    }

    private void openFile() {
        File file = fileChooser("Открыть файл", "*.txt", "*.cpp").showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            codeArea.setText(Files.readString(file.toPath()));
            currentFile = file;
            updateTitle();
        } catch (IOException exception) {
            setOutput(() -> text("Ошибка открытия файла: ") + exception.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            currentFile = fileChooser("Сохранить файл", "*.txt").showSaveDialog(stage);
        }
        if (currentFile == null) {
            return;
        }
        try {
            Files.writeString(currentFile.toPath(), codeArea.getText());
            updateTitle();
        } catch (IOException exception) {
            setOutput(() -> text("Ошибка сохранения файла: ") + exception.getMessage());
        }
    }

    private void changeFontSize(int delta) {
        textFontSize = Math.max(MIN_TEXT_FONT_SIZE, Math.min(MAX_TEXT_FONT_SIZE, textFontSize + delta));
        updateTextFontSize();
    }

    private void updateTextFontSize() {
        String editorFont = fontStyle(textFontSize);
        codeArea.setStyle(editorFont + "-fx-highlight-fill: #e74c3c; -fx-highlight-text-fill: white;");
        lineNumbers.setStyle(editorFont + "-fx-control-inner-background: #f3f3f3;");
        double gutterWidth = Math.max(55, textFontSize * 3.7);
        lineNumbers.setPrefWidth(gutterWidth);
        lineNumbers.setMaxWidth(gutterWidth);
    }

    private String fontStyle(int size) {
        return "-fx-font-family: 'Consolas'; -fx-font-size: " + size + "px;";
    }

    private String text(String russian) {
        return Localization.text(russian, english.get());
    }

    private void localize(StringProperty property, String russian) {
        property.bind(Bindings.createStringBinding(() -> text(russian), english));
    }

    private void setOutput(Supplier<String> message) {
        outputArea.textProperty().unbind();
        outputArea.textProperty().bind(Bindings.createStringBinding(message::get, english));
    }

    private void updateTitle() {
        stage.setTitle("While Analyzer" + (currentFile != null
                ? " - " + currentFile.getName()
                : newDocument ? " - " + text("Новый документ") : ""));
    }

    private void openReport() {
        try {
            getHostServices().showDocument(CourseReport.locate().toUri().toString());
        } catch (IOException | RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, "Текст", text("Не удалось открыть отчет"), exception.getMessage());
        }
    }

    private void showHelp() {
        if (helpStage == null) {
            helpStage = new Stage();
            helpStage.initOwner(stage);
            helpStage.titleProperty().bind(Bindings.createStringBinding(
                    () -> "While Analyzer — " + text("Справка"), english));
            helpStage.setMinWidth(650);
            helpStage.setMinHeight(400);
            TextArea helpText = new TextArea();
            helpText.setEditable(false);
            helpText.setWrapText(true);
            helpText.setStyle(fontStyle(14));
            helpText.textProperty().bind(Bindings.createStringBinding(
                    () -> HelpContent.text(english.get()), english));
            BorderPane helpRoot = new BorderPane(helpText);
            helpRoot.setPadding(new Insets(10));
            Scene scene = new Scene(helpRoot, 900, 650);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    helpStage.close();
                    event.consume();
                }
            });
            helpStage.setScene(scene);
        }
        helpStage.show();
        helpStage.toFront();
    }

    private void showAbout() {
        showAlert(Alert.AlertType.INFORMATION, "О программе", "While Analyzer",
                text("Синтаксический анализатор оператора while языка C++."));
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(text(title));
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
