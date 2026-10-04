package ru.tlp.lexer;

public enum TokenType {
    WHILE,
    BREAK,
    CONTINUE,
    TRUE,
    FALSE,

    IDENTIFIER,
    NUMBER,

    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,

    INCREMENT,
    DECREMENT,

    ASSIGN,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    STAR_ASSIGN,
    SLASH_ASSIGN,
    PERCENT_ASSIGN,

    EQUAL,
    NOT_EQUAL,
    LESS,
    GREATER,
    LESS_EQUAL,
    GREATER_EQUAL,

    NOT,
    AND,
    OR,

    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    SEMICOLON,

    EOF
}
