package ru.tlp.lexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {
    @Test
    void tokenizesSimpleWhile() {
        assertTypes(
                "while (x < 10) { x++; }",

                TokenType.WHILE,
                TokenType.LEFT_PAREN,
                TokenType.IDENTIFIER,
                TokenType.LESS,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.LEFT_BRACE,
                TokenType.IDENTIFIER,
                TokenType.INCREMENT,
                TokenType.SEMICOLON,
                TokenType.RIGHT_BRACE,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesNestedWhileLoops() {
        String code = """
                while (x < 10) {
                    while (y >= 5) {
                        y--;
                    }

                    x++;
                }
                """;

        assertTypes(
                code,

                TokenType.WHILE,
                TokenType.LEFT_PAREN,
                TokenType.IDENTIFIER,
                TokenType.LESS,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.LEFT_BRACE,

                TokenType.WHILE,
                TokenType.LEFT_PAREN,
                TokenType.IDENTIFIER,
                TokenType.GREATER_EQUAL,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.LEFT_BRACE,
                TokenType.IDENTIFIER,
                TokenType.DECREMENT,
                TokenType.SEMICOLON,
                TokenType.RIGHT_BRACE,

                TokenType.IDENTIFIER,
                TokenType.INCREMENT,
                TokenType.SEMICOLON,

                TokenType.RIGHT_BRACE,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesSeveralConditions() {
        String code = """
                while (a < 10 && b != 0 || !finished) {
                    a += 1;
                    b /= 2;
                }
                """;

        assertTypes(
                code,

                TokenType.WHILE,
                TokenType.LEFT_PAREN,

                TokenType.IDENTIFIER,
                TokenType.LESS,
                TokenType.NUMBER,

                TokenType.AND,

                TokenType.IDENTIFIER,
                TokenType.NOT_EQUAL,
                TokenType.NUMBER,

                TokenType.OR,
                TokenType.NOT,
                TokenType.IDENTIFIER,

                TokenType.RIGHT_PAREN,
                TokenType.LEFT_BRACE,

                TokenType.IDENTIFIER,
                TokenType.PLUS_ASSIGN,
                TokenType.NUMBER,
                TokenType.SEMICOLON,

                TokenType.IDENTIFIER,
                TokenType.SLASH_ASSIGN,
                TokenType.NUMBER,
                TokenType.SEMICOLON,

                TokenType.RIGHT_BRACE,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesSeveralIndependentLoops() {
        String code = """
                while (x < 10) {
                    x++;
                }

                while (counter > 0) {
                    counter--;
                }

                while (ready == true) {
                    break;
                }
                """;

        List<Token> tokens = new Lexer(code).tokenize();

        long whileCount = tokens.stream()
                .filter(token -> token.getType() == TokenType.WHILE)
                .count();

        assertEquals(3, whileCount);
        assertEquals(TokenType.EOF, tokens.getLast().getType());
    }

    @Test
    void tokenizesDifferentIdentifierNames() {
        String code =
                "x counter counter2 _value value_3 whileCount breakValue continueFlag";

        List<Token> tokens = new Lexer(code).tokenize();

        for (int i = 0; i < tokens.size() - 1; i++) {
            assertEquals(TokenType.IDENTIFIER, tokens.get(i).getType());
        }

        assertEquals("x", tokens.get(0).getValue());
        assertEquals("counter", tokens.get(1).getValue());
        assertEquals("counter2", tokens.get(2).getValue());
        assertEquals("_value", tokens.get(3).getValue());
        assertEquals("value_3", tokens.get(4).getValue());
        assertEquals("whileCount", tokens.get(5).getValue());
    }

    @Test
    void recognizesKeywordsOnlyWhenNamesMatchExactly() {
        String code =
                "while break continue true false whileCount breakValue trueValue";

        assertTypes(
                code,

                TokenType.WHILE,
                TokenType.BREAK,
                TokenType.CONTINUE,
                TokenType.TRUE,
                TokenType.FALSE,

                TokenType.IDENTIFIER,
                TokenType.IDENTIFIER,
                TokenType.IDENTIFIER,

                TokenType.EOF
        );
    }

    @Test
    void tokenizesAllOperators() {
        String code = """
                + - * / %
                ++ --
                = += -= *= /= %=
                == !=
                < > <= >=
                !
                && ||
                """;

        assertTypes(
                code,

                TokenType.PLUS,
                TokenType.MINUS,
                TokenType.STAR,
                TokenType.SLASH,
                TokenType.PERCENT,

                TokenType.INCREMENT,
                TokenType.DECREMENT,

                TokenType.ASSIGN,
                TokenType.PLUS_ASSIGN,
                TokenType.MINUS_ASSIGN,
                TokenType.STAR_ASSIGN,
                TokenType.SLASH_ASSIGN,
                TokenType.PERCENT_ASSIGN,

                TokenType.EQUAL,
                TokenType.NOT_EQUAL,

                TokenType.LESS,
                TokenType.GREATER,
                TokenType.LESS_EQUAL,
                TokenType.GREATER_EQUAL,

                TokenType.NOT,
                TokenType.AND,
                TokenType.OR,

                TokenType.EOF
        );
    }

    @Test
    void tokenizesBracketsAndSemicolon() {
        assertTypes(
                "(){};",

                TokenType.LEFT_PAREN,
                TokenType.RIGHT_PAREN,
                TokenType.LEFT_BRACE,
                TokenType.RIGHT_BRACE,
                TokenType.SEMICOLON,
                TokenType.EOF
        );
    }

    @Test
    void preservesIdentifierAndNumberValues() {
        List<Token> tokens =
                new Lexer("counter123 = 456;").tokenize();

        assertToken(tokens.get(0), TokenType.IDENTIFIER, "counter123");
        assertToken(tokens.get(1), TokenType.ASSIGN, "=");
        assertToken(tokens.get(2), TokenType.NUMBER, "456");
        assertToken(tokens.get(3), TokenType.SEMICOLON, ";");
        assertToken(tokens.get(4), TokenType.EOF, "");
    }

    @Test
    void ignoresWhitespace() {
        String code = " \t\n while   (\n x\t<  10 ) ";

        assertTypes(
                code,

                TokenType.WHILE,
                TokenType.LEFT_PAREN,
                TokenType.IDENTIFIER,
                TokenType.LESS,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.EOF
        );
    }

    @Test
    void emptyInputProducesOnlyEof() {
        assertTypes("", TokenType.EOF);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@",
            "$",
            "#",
            "&",
            "|",
            "[",
            "]",
            ","
    })
    void rejectsUnknownCharacters(String character) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Lexer("x " + character + " y").tokenize()
        );
    }

    @Test
    void reservedWordIsNotIdentifier() {
        List<Token> tokens =
                new Lexer("while").tokenize();

        assertEquals(TokenType.WHILE, tokens.get(0).getType());
        assertNotEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
    }

    @Test
    void operationBetweenNamesIsSplitIntoTokens() {
        assertTypes(
                "first+second",

                TokenType.IDENTIFIER,
                TokenType.PLUS,
                TokenType.IDENTIFIER,
                TokenType.EOF
        );
    }

    private void assertTypes(String code, TokenType... expectedTypes) {
        List<TokenType> actualTypes =
                new Lexer(code)
                        .tokenize()
                        .stream()
                        .map(Token::getType)
                        .toList();

        assertEquals(List.of(expectedTypes), actualTypes);
    }

    private void assertToken(
            Token token,
            TokenType expectedType,
            String expectedValue
    ) {
        assertEquals(expectedType, token.getType());
        assertEquals(expectedValue, token.getValue());
    }
}