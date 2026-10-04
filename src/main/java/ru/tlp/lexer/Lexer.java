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

            switch (current) {
                case '(' -> addToken(tokens, TokenType.LEFT_PAREN, "(", 1);
                case ')' -> addToken(tokens, TokenType.RIGHT_PAREN, ")", 1);
                case '{' -> addToken(tokens, TokenType.LEFT_BRACE, "{", 1);
                case '}' -> addToken(tokens, TokenType.RIGHT_BRACE, "}", 1);
                case ';' -> addToken(tokens, TokenType.SEMICOLON, ";", 1);

                case '+' -> {
                    if (nextIs('+')) {
                        addToken(tokens, TokenType.INCREMENT, "++", 2);
                    } else if (nextIs('=')) {
                        addToken(tokens, TokenType.PLUS_ASSIGN, "+=", 2);
                    } else {
                        addToken(tokens, TokenType.PLUS, "+", 1);
                    }
                }

                case '-' -> {
                    if (nextIs('-')) {
                        addToken(tokens, TokenType.DECREMENT, "--", 2);
                    } else if (nextIs('=')) {
                        addToken(tokens, TokenType.MINUS_ASSIGN, "-=", 2);
                    } else {
                        addToken(tokens, TokenType.MINUS, "-", 1);
                    }
                }

                case '*' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.STAR_ASSIGN, "*=", 2);
                    } else {
                        addToken(tokens, TokenType.STAR, "*", 1);
                    }
                }

                case '/' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.SLASH_ASSIGN, "/=", 2);
                    } else {
                        addToken(tokens, TokenType.SLASH, "/", 1);
                    }
                }

                case '%' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.PERCENT_ASSIGN, "%=", 2);
                    } else {
                        addToken(tokens, TokenType.PERCENT, "%", 1);
                    }
                }

                case '=' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.EQUAL, "==", 2);
                    } else {
                        addToken(tokens, TokenType.ASSIGN, "=", 1);
                    }
                }

                case '!' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.NOT_EQUAL, "!=", 2);
                    } else {
                        addToken(tokens, TokenType.NOT, "!", 1);
                    }
                }

                case '<' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.LESS_EQUAL, "<=", 2);
                    } else {
                        addToken(tokens, TokenType.LESS, "<", 1);
                    }
                }

                case '>' -> {
                    if (nextIs('=')) {
                        addToken(tokens, TokenType.GREATER_EQUAL, ">=", 2);
                    } else {
                        addToken(tokens, TokenType.GREATER, ">", 1);
                    }
                }

                case '&' -> {
                    if (nextIs('&')) {
                        addToken(tokens, TokenType.AND, "&&", 2);
                    } else {
                        throw unexpectedCharacter(current);
                    }
                }

                case '|' -> {
                    if (nextIs('|')) {
                        addToken(tokens, TokenType.OR, "||", 2);
                    } else {
                        throw unexpectedCharacter(current);
                    }
                }

                default -> throw unexpectedCharacter(current);
            }
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

        return new Token(
                TokenType.NUMBER,
                input.substring(start, position)
        );
    }

    private boolean nextIs(char expected) {
        return position + 1 < input.length()
                && input.charAt(position + 1) == expected;
    }

    private void addToken(
            List<Token> tokens,
            TokenType type,
            String value,
            int length
    ) {
        tokens.add(new Token(type, value));
        position += length;
    }

    private IllegalArgumentException unexpectedCharacter(char character) {
        return new IllegalArgumentException(
                "Неожиданный символ '" + character
                        + "' в позиции " + position
        );
    }
}