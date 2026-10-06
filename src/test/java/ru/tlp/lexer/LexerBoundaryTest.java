package ru.tlp.lexer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.tlp.analyzer.Diagnostic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerBoundaryTest {
    @ParameterizedTest
    @ValueSource(strings = { "_", "_1", "x_", "x123", "счетчик", "готов_2", "变量", "While",
            "WHILE", "Break", "Continue", "TRUE", "False", "while1", "break_", "continue2" })
    void preservesIdentifiersAndTheirOffsets(String identifier) {
        String code = " \r\n\t" + identifier + " ";
        var tokens = new Lexer(code).tokenize();
        assertEquals(2, tokens.size());
        assertEquals(TokenType.IDENTIFIER, tokens.getFirst().getType());
        assertEquals(identifier, tokens.getFirst().getValue());
        assertEquals(4, tokens.getFirst().getPosition());
        assertEquals(code.length(), tokens.getLast().getPosition());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "00", "123", "99999999999999999999999999999", "١٢٣", "１２３" })
    void acceptsDigitSequencesWithoutIntegerConversion(String number) {
        var tokens = new Lexer(number).tokenize();
        assertEquals(TokenType.NUMBER, tokens.getFirst().getType());
        assertEquals(number, tokens.getFirst().getValue());
        assertEquals(2, tokens.size());
    }

    @ParameterizedTest
    @ValueSource(strings = { "@", "#", "$", "&", "|", "[", "]", ",", ".", ":", "?", "~", "^", "\"", "'", "\\" })
    void collectingModeReportsBadCharacterAndKeepsFollowingToken(String bad) {
        List<Diagnostic> errors = new ArrayList<>();
        String code = "x " + bad + " y";
        var tokens = new Lexer(code).tokenize(errors::add);
        assertEquals(1, errors.size());
        assertEquals(2, errors.getFirst().position());
        assertEquals(1, errors.getFirst().length());
        assertEquals(List.of(TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.EOF),
                tokens.stream().map(Token::getType).toList());
        assertEquals(4, tokens.get(1).getPosition());
        assertThrows(IllegalArgumentException.class, () -> new Lexer(code).tokenize());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\n", "\r\n", "\t", "\f", " \t\r\n\f " })
    void whitespaceOnlyInputHasEofAtOriginalEnd(String code) {
        var tokens = new Lexer(code).tokenize();
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.getFirst().getType());
        assertEquals(code.length(), tokens.getFirst().getPosition());
    }
}
