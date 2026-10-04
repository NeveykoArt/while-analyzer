package ru.tlp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import ru.tlp.analyzer.AnalysisResult;
import ru.tlp.analyzer.Analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private final Analyzer analyzer = new Analyzer();

    private TextArea codeArea;
    private TextArea outputArea;
    private TextArea lineNumbers;

    private Stage stage;
    private File currentFile;

    private static final double MIN_UI_SCALE = 0.7;
    private static final double MAX_UI_SCALE = 1.8;
    private static final double UI_SCALE_STEP = 0.1;

    private static final double BASE_TOOLTIP_FONT_SIZE = 13;
    private static final double BASE_MENU_FONT_SIZE = 13;

    private double uiScale = 1.0;

    private BorderPane root;
    private Pane zoomPane;
    private MenuBar menuBar;

    private final Scale scaleTransform = new Scale(1, 1, 0, 0);

    private final List<Tooltip> tooltips = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        root = new BorderPane();

        root.setTop(createTop());
        root.setCenter(createEditor());

        root.setManaged(false);
        root.getTransforms().add(scaleTransform);

        zoomPane = new Pane(root);

        Scene scene = new Scene(zoomPane, 1000, 700);

        scene.widthProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateScaledLayout(scene)
        );

        scene.heightProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateScaledLayout(scene)
        );

        scene.addEventFilter(
                ScrollEvent.SCROLL,
                event -> handleZoom(event, scene)
        );

        stage.setTitle("While Analyzer");
        stage.setScene(scene);
        stage.show();

        updateScaledLayout(scene);
        updatePopupMenusScale();
        updateTooltipsScale();
    }

    private void handleZoom(ScrollEvent event, Scene scene) {
        boolean modifierPressed =
                event.isControlDown() || event.isMetaDown();

        if (!modifierPressed || event.getDeltaY() == 0) {
            return;
        }

        if (event.getDeltaY() > 0) {
            uiScale += UI_SCALE_STEP;
        } else {
            uiScale -= UI_SCALE_STEP;
        }

        uiScale = Math.max(
                MIN_UI_SCALE,
                Math.min(MAX_UI_SCALE, uiScale)
        );

        scaleTransform.setX(uiScale);
        scaleTransform.setY(uiScale);

        updatePopupMenusScale();
        updateTooltipsScale();
        updateScaledLayout(scene);

        event.consume();
    }

    private void updateScaledLayout(Scene scene) {
        double width = scene.getWidth() / uiScale;
        double height = scene.getHeight() / uiScale;

        root.resizeRelocate(
                0,
                0,
                width,
                height
        );
    }

    private void updatePopupMenusScale() {
        if (menuBar == null) {
            return;
        }

        for (Menu menu : menuBar.getMenus()) {
            updateMenuItemsScale(menu);
        }
    }

    private void updateMenuItemsScale(Menu menu) {
        double fontSize = BASE_MENU_FONT_SIZE * uiScale;

        double verticalPadding = 5 * uiScale;
        double horizontalPadding = 10 * uiScale;

        for (MenuItem item : menu.getItems()) {

            if (item instanceof SeparatorMenuItem) {
                item.setStyle(
                        "-fx-padding: "
                                + (3 * uiScale)
                                + "px 0px;"
                );
            } else {
                item.setStyle(
                        "-fx-font-size: " + fontSize + "px;"
                                + "-fx-padding: "
                                + verticalPadding + "px "
                                + horizontalPadding + "px;"
                );
            }

            if (item instanceof Menu subMenu) {
                updateMenuItemsScale(subMenu);
            }
        }
    }

    private VBox createTop() {
        menuBar = createMenuBar();

        ToolBar toolBar = createToolBar();

        return new VBox(menuBar, toolBar);
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("Файл");

        MenuItem newItem = new MenuItem("Создать");
        MenuItem openItem = new MenuItem("Открыть");
        MenuItem saveItem = new MenuItem("Сохранить");
        MenuItem exitItem = new MenuItem("Выход");

        newItem.setOnAction(e -> newFile());
        openItem.setOnAction(e -> openFile());
        saveItem.setOnAction(e -> saveFile());
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newItem,
                openItem,
                saveItem,
                new SeparatorMenuItem(),
                exitItem
        );

        Menu editMenu = new Menu("Правка");

        MenuItem undoItem = new MenuItem("Отменить");
        MenuItem redoItem = new MenuItem("Повторить");
        MenuItem cutItem = new MenuItem("Вырезать");
        MenuItem copyItem = new MenuItem("Копировать");
        MenuItem pasteItem = new MenuItem("Вставить");

        undoItem.setOnAction(e -> codeArea.undo());
        redoItem.setOnAction(e -> codeArea.redo());
        cutItem.setOnAction(e -> codeArea.cut());
        copyItem.setOnAction(e -> codeArea.copy());
        pasteItem.setOnAction(e -> codeArea.paste());

        editMenu.getItems().addAll(
                undoItem,
                redoItem,
                new SeparatorMenuItem(),
                cutItem,
                copyItem,
                pasteItem
        );

        Menu textMenu = new Menu("Текст");

        MenuItem grammarItem =
                new MenuItem("Грамматика");

        MenuItem classificationItem =
                new MenuItem("Классификация грамматики");

        MenuItem analysisMethodItem =
                new MenuItem("Метод анализа");

        MenuItem diagnosticsItem =
                new MenuItem("Диагностика ошибок");

        MenuItem testItem =
                new MenuItem("Тестовый пример");

        MenuItem literatureItem =
                new MenuItem("Список литературы");

        MenuItem sourceItem =
                new MenuItem("Исходный код программы");

        textMenu.getItems().addAll(
                grammarItem,
                classificationItem,
                analysisMethodItem,
                diagnosticsItem,
                testItem,
                literatureItem,
                sourceItem
        );

        Menu runMenu = new Menu("Пуск");

        MenuItem runItem = new MenuItem("Анализ");

        runItem.setOnAction(e -> analyze());

        runMenu.getItems().add(runItem);

        Menu helpMenu = new Menu("Справка");

        MenuItem helpItem =
                new MenuItem("Вызов справки");

        MenuItem aboutItem =
                new MenuItem("О программе");

        aboutItem.setOnAction(e -> showAbout());

        helpMenu.getItems().addAll(
                helpItem,
                new SeparatorMenuItem(),
                aboutItem
        );

        Menu localizationMenu =
                new Menu("Локализация");

        RadioMenuItem russianItem =
                new RadioMenuItem("Русская");

        RadioMenuItem englishItem =
                new RadioMenuItem("English");

        ToggleGroup languageGroup =
                new ToggleGroup();

        russianItem.setToggleGroup(languageGroup);
        englishItem.setToggleGroup(languageGroup);

        russianItem.setSelected(true);

        localizationMenu.getItems().addAll(
                russianItem,
                englishItem
        );

        Menu viewMenu = new Menu("Вид");

        Menu fontMenu = new Menu("Размер текста");

        MenuItem increaseFont =
                new MenuItem("Увеличить шрифт");

        MenuItem decreaseFont =
                new MenuItem("Уменьшить шрифт");

        increaseFont.setOnAction(
                e -> changeFontSize(1)
        );

        decreaseFont.setOnAction(
                e -> changeFontSize(-1)
        );

        fontMenu.getItems().addAll(
                increaseFont,
                decreaseFont
        );

        viewMenu.getItems().add(fontMenu);

        return new MenuBar(
                fileMenu,
                editMenu,
                textMenu,
                runMenu,
                helpMenu,
                localizationMenu,
                viewMenu
        );
    }

    private ToolBar createToolBar() {
        Button newButton =
                createButton("＋", "Создать");

        Button openButton =
                createButton("📂", "Открыть");

        Button saveButton =
                createButton("💾", "Сохранить");

        Button undoButton =
                createButton("↶", "Отменить");

        Button redoButton =
                createButton("↷", "Повторить");

        Button cutButton =
                createButton("✂", "Вырезать");

        Button copyButton =
                createButton("⧉", "Копировать");

        Button pasteButton =
                createButton("▣", "Вставить");

        Button runButton =
                createButton("▶", "Пуск");

        Button helpButton =
                createButton("?", "Справка");

        Button aboutButton =
                createButton("i", "О программе");

        newButton.setOnAction(e -> newFile());
        openButton.setOnAction(e -> openFile());
        saveButton.setOnAction(e -> saveFile());

        undoButton.setOnAction(e -> codeArea.undo());
        redoButton.setOnAction(e -> codeArea.redo());

        cutButton.setOnAction(e -> codeArea.cut());
        copyButton.setOnAction(e -> codeArea.copy());
        pasteButton.setOnAction(e -> codeArea.paste());

        runButton.setOnAction(e -> analyze());
        aboutButton.setOnAction(e -> showAbout());

        return new ToolBar(
                newButton,
                openButton,
                saveButton,

                new Separator(),

                undoButton,
                redoButton,

                new Separator(),

                cutButton,
                copyButton,
                pasteButton,

                new Separator(),

                runButton,
                helpButton,
                aboutButton
        );
    }

    private Button createButton(
            String text,
            String tooltipText
    ) {
        Button button = new Button(text);

        Tooltip tooltip = new Tooltip(tooltipText);

        tooltips.add(tooltip);

        button.setTooltip(tooltip);

        button.setPrefWidth(42);
        button.setPrefHeight(34);

        updateTooltipScale(tooltip);

        return button;
    }

    private void updateTooltipScale(Tooltip tooltip) {
        double fontSize =
                BASE_TOOLTIP_FONT_SIZE * uiScale;

        double padding = 6 * uiScale;

        tooltip.setStyle(
                "-fx-font-size: " + fontSize + "px;"
                        + "-fx-padding: " + padding + "px;"
        );
    }

    private void updateTooltipsScale() {
        for (Tooltip tooltip : tooltips) {
            updateTooltipScale(tooltip);
        }
    }

    private SplitPane createEditor() {
        codeArea = new TextArea();

        codeArea.setWrapText(false);

        codeArea.setStyle(
                "-fx-font-family: 'Consolas';"
                        + "-fx-font-size: 15px;"
        );

        lineNumbers = new TextArea("1");

        lineNumbers.setEditable(false);
        lineNumbers.setFocusTraversable(false);

        lineNumbers.setPrefWidth(55);
        lineNumbers.setMaxWidth(55);

        lineNumbers.setStyle(
                "-fx-font-family: 'Consolas';"
                        + "-fx-font-size: 15px;"
                        + "-fx-control-inner-background: #f3f3f3;"
        );

        codeArea.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateLineNumbers()
        );

        lineNumbers.scrollTopProperty()
                .bindBidirectional(
                        codeArea.scrollTopProperty()
                );

        HBox editorBox =
                new HBox(lineNumbers, codeArea);

        HBox.setHgrow(
                codeArea,
                Priority.ALWAYS
        );

        outputArea = new TextArea();

        outputArea.setEditable(false);
        outputArea.setWrapText(true);

        outputArea.setStyle(
                "-fx-font-family: 'Consolas';"
                        + "-fx-font-size: 14px;"
        );

        VBox outputBox = new VBox();

        Label outputLabel =
                new Label("Результат анализа");

        outputLabel.setPadding(
                new Insets(6)
        );

        VBox.setVgrow(
                outputArea,
                Priority.ALWAYS
        );

        outputBox.getChildren().addAll(
                outputLabel,
                outputArea
        );

        SplitPane splitPane =
                new SplitPane(
                        editorBox,
                        outputBox
                );

        splitPane.setOrientation(
                javafx.geometry.Orientation.VERTICAL
        );

        splitPane.setDividerPositions(0.68);

        return splitPane;
    }

    private void updateLineNumbers() {
        int lines =
                codeArea.getText()
                        .split("\n", -1)
                        .length;

        StringBuilder numbers =
                new StringBuilder();

        for (int i = 1; i <= lines; i++) {
            numbers
                    .append(i)
                    .append(System.lineSeparator());
        }

        lineNumbers.setText(
                numbers.toString()
        );
    }

    private void analyze() {
        AnalysisResult result =
                analyzer.analyze(
                        codeArea.getText()
                );

        outputArea.setText(
                result.message()
        );
    }

    private void newFile() {
        codeArea.clear();
        outputArea.clear();

        currentFile = null;

        stage.setTitle(
                "While Analyzer - Новый документ"
        );
    }

    private void openFile() {
        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Открыть файл"
        );

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Текстовые файлы",
                        "*.txt",
                        "*.cpp"
                )
        );

        File file =
                chooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        try {
            codeArea.setText(
                    Files.readString(
                            file.toPath()
                    )
            );

            currentFile = file;

            stage.setTitle(
                    "While Analyzer - "
                            + file.getName()
            );

        } catch (IOException exception) {
            outputArea.setText(
                    "Ошибка открытия файла: "
                            + exception.getMessage()
            );
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            FileChooser chooser =
                    new FileChooser();

            chooser.setTitle(
                    "Сохранить файл"
            );

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Текстовые файлы",
                            "*.txt"
                    )
            );

            currentFile =
                    chooser.showSaveDialog(stage);
        }

        if (currentFile == null) {
            return;
        }

        try {
            Files.writeString(
                    currentFile.toPath(),
                    codeArea.getText()
            );

            stage.setTitle(
                    "While Analyzer - "
                            + currentFile.getName()
            );

        } catch (IOException exception) {
            outputArea.setText(
                    "Ошибка сохранения файла: "
                            + exception.getMessage()
            );
        }
    }

    private void changeFontSize(int delta) {
        // Реализуем отдельно позже.
    }

    private void showAbout() {
        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(
                "О программе"
        );

        alert.setHeaderText(
                "While Analyzer"
        );

        alert.setContentText(
                "Синтаксический анализатор "
                        + "оператора while языка C++."
        );

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}