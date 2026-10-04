package ru.tlp.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String input;
    private int position = 0;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (Character.isLetter(current) || current == '_') {
                tokens.add(readIdentifier());
                continue;
            }

            if (Character.isDigit(current)) {
                tokens.add(readNumber());
                continue;
            }

            position++;
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private Token readIdentifier() {
            int start = position;

            while (position < input.length()) {
                char current = input.charAt(position);

                if (!Character.isLetterOrDigit(current) && current != '_') {
                    break;
                }

                position++;
            }

            String value = input.substring(start, position);

            TokenType type = switch (value) {
                case "while" -> TokenType.WHILE;
                case "break" -> TokenType.BREAK;
                case "continue" -> TokenType.CONTINUE;
                case "true" -> TokenType.TRUE;
                case "false" -> TokenType.FALSE;
                default -> TokenType.IDENTIFIER;
            };

            return new Token(type, value);
        }

        private Token readNumber() {
        int start = position;

        while (position < input.length()
                && Character.isDigit(input.charAt(position))) {
            position++;
        }

        String value = input.substring(start, position);

        return new Token(TokenType.NUMBER, value);
    }
}