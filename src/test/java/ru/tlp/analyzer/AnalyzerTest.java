package ru.tlp.analyzer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzerTest {

    private final Analyzer analyzer = new Analyzer();

    @Test
    void highlightsLeadingAndTrailingGarbageWithoutInventingLoopHeaderErrors() {
        String prefix = "dsfgkljfhdgkjfdg dfgdfgdfgfdg";
        String bodyIdentifier = "fdfdfdfdf";
        String code = prefix + " while (true) { break; " + bodyIdentifier + " } " + bodyIdentifier;
        AnalysisResult result = analyzer.analyze(code);
        assertEquals(3, result.errors().size(), result.message());
        assertEquals(0, result.errors().get(0).position());
        assertEquals(prefix.length(), result.errors().get(0).length());
        assertEquals(code.indexOf(bodyIdentifier) + bodyIdentifier.length(), result.errors().get(1).position());
        assertEquals(0, result.errors().get(1).length());
        assertTrue(result.errors().get(1).message().contains("SEMICOLON"));
        assertEquals(code.lastIndexOf(bodyIdentifier), result.errors().get(2).position());
        assertEquals(bodyIdentifier.length(), result.errors().get(2).length());
    }

    @Test
    void discardsPrefixAndStillReportsIndependentErrorsInRealLoop() {
        String code = "a b c while (break) { x = ; } tail extra";
        AnalysisResult result = analyzer.analyze(code);
        assertEquals(4, result.errors().size(), result.message());
        assertEquals(5, result.errors().getFirst().length());
        assertEquals(code.indexOf("break"), result.errors().get(1).position());
        assertEquals(code.indexOf("tail"), result.errors().getLast().position());
        assertEquals("tail extra".length(), result.errors().getLast().length());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = { "break", "continue" })
    void reportsOneErrorForControlStatementInNestedLoopCondition(String keyword) {
        String code = "while (true)\n\twhile (" + keyword + ") {\n\t\tb;\n\t}";
        AnalysisResult result = analyzer.analyze(code);
        assertEquals(1, result.errors().size(), result.message());
        assertEquals(code.indexOf(keyword), result.errorPosition());
        assertEquals(keyword.length(), result.errorLength());
    }

    @Test
    void retainsIndependentBodyErrorAfterInvalidConditionKeyword() {
        String code = "while (true) while ((break)) { b = ; continue; }";
        AnalysisResult result = analyzer.analyze(code);
        assertEquals(2, result.errors().size(), result.message());
        assertEquals(code.indexOf("break"), result.errors().getFirst().position());
        assertEquals(code.indexOf("= ;") + 1, result.errors().getLast().position());
    }

    @Test
    void continuesAfterMissingWhileKeywordAndCollectsLongErrorLists() {
        AnalysisResult result = analyzer.analyze("(x) { x = ; y = ; }");
        assertEquals(3, result.errors().size(), result.message());
        assertEquals(100, analyzer.analyze("while (x) { " + "x = ; ".repeat(100) + "}").errors().size());
    }

    @Test
    void collectsIndependentStatementErrors() {
        AnalysisResult result = analyzer.analyze("while (x) { x = ; y = ; z++; }");
        assertFalse(result.success());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().get(0).position() < result.errors().get(1).position());
        assertEquals(result.errors().getFirst().position(), result.errorPosition());
    }

    @Test
    void retainsStatementsAfterMissingSemicolons() {
        AnalysisResult result = analyzer.analyze("while (x) { x++ y++ break continue; }");
        assertEquals(3, result.errors().size(), result.message());
    }

    @Test
    void recoversFromInvalidConditionAndNestedBlockErrors() {
        AnalysisResult result = analyzer.analyze(
                "while () { x = ; while (y) { y = ; } z = ; }");
        assertEquals(4, result.errors().size(), result.message());
    }

    @Test
    void combinesLexicalAndSyntaxErrorsInSourceOrder() {
        String code = "while (x) { @ x = ; # y = ; }";
        AnalysisResult result = analyzer.analyze(code);
        assertEquals(4, result.errors().size(), result.message());
        assertEquals(code.indexOf('@'), result.errors().get(0).position());
        assertEquals(code.indexOf('#'), result.errors().get(2).position());
        assertEquals(1, result.errorLength());
    }

    @Test
    void reportsMissingClosingBracesAndTerminatesOnMalformedInput() {
        assertEquals(2, analyzer.analyze("while (x) { while (y) { y++; ").errors().size());
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            for (String code : new String[] { "", "}", "{{{{", "while (", "while ()", ";;;", "while (x) { + + + ; }" }) {
                assertFalse(analyzer.analyze(code).success(), code);
            }
        });
    }

    @Test
    void acceptsValidCode() {
        AnalysisResult result = analyzer.analyze("""
                while (x < 10) {
                    x++;
                }
                """);

        assertTrue(result.success());
        assertEquals("Синтаксис корректен", result.message());
    }

    @Test
    void rejectsSyntaxError() {
        AnalysisResult result = analyzer.analyze("""
                while (x < 10) {
                    x++
                }
                """);

        assertFalse(result.success());
        assertNotNull(result.message());
    }

    @Test
    void rejectsLexicalError() {
        AnalysisResult result = analyzer.analyze("""
                while (x @ 10) {
                    x++;
                }
                """);

        assertFalse(result.success());
        assertNotNull(result.message());
    }
}
