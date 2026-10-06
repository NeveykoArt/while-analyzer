package ru.tlp.parser;

import org.junit.jupiter.api.Test;
import ru.tlp.lexer.Lexer;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static ru.tlp.lexer.TokenType.*;

class IronsParserTest {
    @Test
    void keepsUnstartedLoopInResidualDerivationUntilWhileIsFound() {
        IronsParser parser = parser("a b while (true) {}");
        var errors = parser.parse();
        assertEquals(1, errors.size());
        var recovery = parser.recoveries().getFirst();
        assertEquals(List.of("Loop", "Terminal[type=EOF]"), recovery.residualSymbols());
        assertEquals(java.util.Set.of(WHILE, EOF), recovery.admissibleTokens());
        assertTrue(recovery.completion().isEmpty());
        assertEquals(0, recovery.start());
        assertEquals(3, recovery.end());
    }

    @Test
    void discardsTheWholeInvalidFragmentAndResumesAtDerivableOperand() {
        String code = "while (x) { x = = = 5; y = ; }";
        IronsParser parser = parser(code);
        var errors = parser.parse();
        assertEquals(2, errors.size());
        assertEquals(code.indexOf("= = 5"), errors.getFirst().position());
        assertEquals(3, errors.getFirst().length());
        var recovery = parser.recoveries().getFirst();
        assertTrue(recovery.admissibleTokens().contains(NUMBER));
        assertEquals("Term", recovery.resumedBranch());
        assertTrue(recovery.completion().isEmpty());
        assertEquals("= =", code.substring(recovery.start(), recovery.end()));
    }

    @Test
    void completesMissingOperandVirtuallyWithoutDiscardingSemicolon() {
        String code = "while (x) { x = ; y++; }";
        var tokens = new Lexer(code).tokenize();
        var originalTokens = List.copyOf(tokens);
        IronsParser parser = new IronsParser(tokens);
        var errors = parser.parse();
        assertEquals(1, errors.size());
        assertEquals(code.indexOf("= ;") + 1, errors.getFirst().position());
        assertEquals(0, errors.getFirst().length());
        var recovery = parser.recoveries().getFirst();
        assertEquals(List.of(IDENTIFIER), recovery.completion());
        assertEquals("Statement", recovery.resumedBranch());
        assertEquals(recovery.start(), recovery.end());
        assertEquals(originalTokens, tokens);
    }

    @Test
    void residualSetIncludesContinuationOfOuterBranches() {
        String code = "while (x) { x++ y++; }";
        IronsParser parser = parser(code);
        var errors = parser.parse();
        assertEquals(1, errors.size());
        assertEquals(0, errors.getFirst().length());
        assertEquals(code.indexOf("x++") + 3, errors.getFirst().position());
        var recovery = parser.recoveries().getFirst();
        assertTrue(recovery.admissibleTokens().contains(IDENTIFIER));
        assertTrue(recovery.admissibleTokens().contains(RIGHT_BRACE));
        assertEquals(List.of(SEMICOLON), recovery.completion());
        assertEquals("Statements", recovery.resumedBranch());
    }

    @Test
    void recoversInsideParenthesesWithoutLosingLaterConditionAndBodyErrors() {
        IronsParser parser = parser("while ((x + = = 2) && y >) { z = ; }");
        assertEquals(3, parser.parse().size());
        assertEquals(3, parser.recoveries().size());
    }

    @Test
    void handlesDamagedRepetitionAndKeepsFollowingStatement() {
        IronsParser parser = parser("while (x) { = = y++; z = ; }");
        var errors = parser.parse();
        assertEquals(2, errors.size());
        assertEquals(3, errors.getFirst().length());
        assertEquals("Block", parser.recoveries().getFirst().resumedBranch());
    }

    @Test
    void grammarRecoveryTerminatesOnRandomMalformedInputs() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Random random = new Random(42);
            String[] pieces = { "while", "x", "1", "(", ")", "{", "}", ";", "=", "+", "*", "&&", "!", "break" };
            for (int example = 0; example < 500; example++) {
                StringBuilder code = new StringBuilder();
                for (int i = 0; i < 30; i++) {
                    code.append(pieces[random.nextInt(pieces.length)]).append(' ');
                }
                parser(code.toString()).parse();
            }
        });
    }

    private static IronsParser parser(String code) {
        return new IronsParser(new Lexer(code).tokenize());
    }
}
