package ru.tlp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class HelpContent {
    private static final String RUSSIAN = load("help-ru.txt");
    private static final String ENGLISH = load("help-en.txt");

    private HelpContent() {}

    static String text(boolean english) {
        return english ? ENGLISH : RUSSIAN;
    }

    private static String load(String file) {
        try (var stream = HelpContent.class.getResourceAsStream("/ru/tlp/" + file)) {
            if (stream == null) {
                throw new IllegalStateException("Missing help resource: " + file);
            }
            String document = new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
            if (document.isBlank()) {
                throw new IllegalStateException("Empty help resource: " + file);
            }
            return document;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read help resource: " + file, exception);
        }
    }
}