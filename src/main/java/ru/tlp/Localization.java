package ru.tlp;

import java.util.Map;
import java.util.stream.Collectors;
import ru.tlp.analyzer.AnalysisResult;

final class Localization {
    private Localization() {}

    private static final Map<String, String> ENGLISH = Map.ofEntries(
            Map.entry("Файл", "File"),
            Map.entry("Создать", "New"),
            Map.entry("Открыть", "Open"),
            Map.entry("Сохранить", "Save"),
            Map.entry("Выход", "Exit"),
            Map.entry("Правка", "Edit"),
            Map.entry("Отменить", "Undo"),
            Map.entry("Повторить", "Redo"),
            Map.entry("Вырезать", "Cut"),
            Map.entry("Копировать", "Copy"),
            Map.entry("Вставить", "Paste"),
            Map.entry("Текст", "Text"),
            Map.entry("Не удалось открыть отчет", "Could not open the report"),
            Map.entry("Пуск", "Run"),
            Map.entry("Анализ", "Analyze"),
            Map.entry("Справка", "Help"),
            Map.entry("Вызов справки", "Show help"),
            Map.entry("О программе", "About"),
            Map.entry("Локализация", "Language"),
            Map.entry("Вид", "View"),
            Map.entry("Размер текста", "Text size"),
            Map.entry("Увеличить шрифт", "Increase font size"),
            Map.entry("Уменьшить шрифт", "Decrease font size"),
            Map.entry("Результат анализа", "Analysis result"),
            Map.entry("Новый документ", "New document"),
            Map.entry("Открыть файл", "Open file"),
            Map.entry("Сохранить файл", "Save file"),
            Map.entry("Текстовые файлы", "Text files"),
            Map.entry("Ошибка открытия файла: ", "Error opening file: "),
            Map.entry("Ошибка сохранения файла: ", "Error saving file: "),
            Map.entry("Синтаксический анализатор оператора while языка C++.",
                    "Syntax analyzer for the C++ while statement."),
            Map.entry("Синтаксис корректен", "Syntax is correct")
    );

    static String text(String russian, boolean english) {
        return english ? ENGLISH.getOrDefault(russian, russian) : russian;
    }

    static String diagnostic(String message, boolean english) {
        if (message.contains("\n")) {
            return message.lines().map(line -> diagnostic(line, english))
                    .collect(Collectors.joining("\n"));
        }
        if (!english) {
            return message;
        }
        if (message.equals("Синтаксис корректен")) {
            return text(message, true);
        }
        if (message.startsWith("Неожиданный символ '")) {
            return "Unexpected character '" + message.substring("Неожиданный символ '".length())
                    .replace("' в позиции ", "' at position ");
        }
        int details = message.indexOf(". Получено: ");
        if (details < 0) {
            return message;
        }
        String expected = message.substring(0, details);
        String translated = switch (expected) {
            case "Ожидался идентификатор, число, true, false или '('" ->
                    "Expected an identifier, number, true, false or '('";
            default -> expected.startsWith("Ожидался токен ")
                    ? "Expected token " + expected.substring("Ожидался токен ".length())
                    : expected;
        };
        return translated + ". Received: "
                + message.substring(details + ". Получено: ".length())
                        .replace(", индекс токена: ", ", token index: ");
    }

    static String analysis(AnalysisResult result, String code, boolean english) {
        if (result.success()) {
            return diagnostic(result.message(), english);
        }
        StringBuilder output = new StringBuilder(english ? "Errors: " : "Ошибок: ");
        output.append(result.errors().size());
        int number = 0;
        for (var error : result.errors()) {
            int offset = Math.max(0, Math.min(code.length(), error.position()));
            int line = 1;
            int column = 1;
            for (int i = 0; i < offset; i++) {
                if (code.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            output.append("\n").append(++number).append(". ")
                    .append(english ? "Line " : "Строка ").append(line)
                    .append(english ? ", column " : ", столбец ").append(column)
                    .append(": ").append(diagnostic(error.message(), english));
        }
        return output.toString();
    }
}
