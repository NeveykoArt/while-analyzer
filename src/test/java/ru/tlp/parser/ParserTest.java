package ru.tlp.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.tlp.lexer.Lexer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {

    @Test
    void parsesSimpleWhile() {
        assertValid("""
                while (x < 10) {
                    x++;
                }
                """);
    }

    @Test
    void parsesEmptyBlock() {
        assertValid("""
                while (true) {
                }
                """);
    }

    @Test
    void parsesExpressionAsBody() {
        assertValid("""
                while (x < 10)
                    x++;
                """);
    }

    @Test
    void parsesWhileAsBody() {
        assertValid("""
                while (x < 10)
                    while (y > 0)
                        y--;
                """);
    }

    @Test
    void parsesNestedWhileLoops() {
        assertValid("""
                while (x < 10) {
                    while (y > 0) {
                        y--;

                        while (z != 5) {
                            z++;
                        }
                    }

                    x++;
                }
                """);
    }

    @Test
    void parsesBreakAndContinueInsideBlock() {
        assertValid("""
                while (x < 10) {
                    x++;
                    continue;
                    break;
                }
                """);
    }

    @Test
    void parsesComplexCondition() {
        assertValid("""
                while (x < 10 && y >= 5 || ready == true) {
                    x++;
                }
                """);
    }

    @Test
    void parsesNotOperator() {
        assertValid("""
                while (!finished) {
                    finished = true;
                }
                """);
    }

    @Test
    void parsesParenthesizedCondition() {
        assertValid("""
                while ((x < 10 || y > 20) && ready == true) {
                    x++;
                }
                """);
    }

    @Test
    void parsesArithmeticExpressions() {
        assertValid("""
                while (x + 5 * y < 100) {
                    result = x + y * 2 - 5 / value % 3;
                }
                """);
    }

    @Test
    void parsesAllAssignmentOperators() {
        assertValid("""
                while (x < 100) {
                    x = 10;
                    x += 5;
                    x -= 2;
                    x *= 3;
                    x /= 2;
                    x %= 7;
                }
                """);
    }

    @Test
    void parsesPrefixIncrementAndDecrement() {
        assertValid("""
                while (x < 10) {
                    ++x;
                    --y;
                }
                """);
    }

    @Test
    void parsesPostfixIncrementAndDecrement() {
        assertValid("""
                while (x < 10) {
                    x++;
                    y--;
                }
                """);
    }

    @Test
    void parsesDifferentVariableNames() {
        assertValid("""
                while (counter2 < max_value && _ready == true) {
                    counter2++;
                    max_value -= 1;
                    _ready = false;
                }
                """);
    }

    @Test
    void parsesBooleanCondition() {
        assertValid("""
                while (true) {
                    flag = false;
                }
                """);
    }

    @Test
    void parsesRelationWithoutComparisonOperator() {
        assertValid("""
                while (flag) {
                    flag = false;
                }
                """);
    }

    @Test
    void rejectsMissingLeftParenthesis() {
        assertInvalid("""
                while x < 10) {
                    x++;
                }
                """);
    }

    @Test
    void rejectsMissingRightParenthesis() {
        assertInvalid("""
                while (x < 10 {
                    x++;
                }
                """);
    }

    @Test
    void rejectsMissingOpeningBrace() {
        assertInvalid("""
                while (x < 10)
                    x++;
                    y++;
                }
                """);
    }

    @Test
    void rejectsMissingClosingBrace() {
        assertInvalid("""
                while (x < 10) {
                    x++;
                """);
    }

    @Test
    void rejectsMissingSemicolon() {
        assertInvalid("""
                while (x < 10) {
                    x++
                }
                """);
    }

    @Test
    void rejectsEmptyCondition() {
        assertInvalid("""
                while () {
                    x++;
                }
                """);
    }

    @Test
    void rejectsConditionEndingWithAnd() {
        assertInvalid("""
                while (x < 10 &&) {
                    x++;
                }
                """);
    }

    @Test
    void rejectsConditionEndingWithOr() {
        assertInvalid("""
                while (x < 10 ||) {
                    x++;
                }
                """);
    }

    @Test
    void rejectsMissingRightSideOfComparison() {
        assertInvalid("""
                while (x <) {
                    x++;
                }
                """);
    }

    @Test
    void rejectsMissingRightSideOfAssignment() {
        assertInvalid("""
                while (x < 10) {
                    x =;
                }
                """);
    }

    @Test
    void rejectsTwoArithmeticOperatorsInRow() {
        assertInvalid("""
                while (x < 10) {
                    x = y + * 5;
                }
                """);
    }

    @Test
    void rejectsKeywordAsVariableName() {
        assertInvalid("""
                while (x < 10) {
                    while = 5;
                }
                """);
    }

    @Test
    void rejectsBreakAsVariableName() {
        assertInvalid("""
                while (x < 10) {
                    break = 5;
                }
                """);
    }

    @Test
    void rejectsContinueAsVariableName() {
        assertInvalid("""
                while (x < 10) {
                    continue = 5;
                }
                """);
    }

    @Test
    void rejectsOperatorInsideAssignmentTarget() {
        assertInvalid("""
                while (x < 10) {
                    first+second = 5;
                }
                """);
    }

    @Test
    void rejectsIncrementWithoutIdentifier() {
        assertInvalid("""
                while (x < 10) {
                    ++5;
                }
                """);
    }

    @Test
    void rejectsDecrementWithoutIdentifier() {
        assertInvalid("""
                while (x < 10) {
                    --true;
                }
                """);
    }

    @Test
    void rejectsDoubleNot() {
        assertInvalid("""
                while (!!flag) {
                    flag = false;
                }
                """);
    }

    @Test
    void rejectsBreakWithoutSemicolon() {
        assertInvalid("""
                while (x < 10) {
                    break
                }
                """);
    }

    @Test
    void rejectsContinueWithoutSemicolon() {
        assertInvalid("""
                while (x < 10) {
                    continue
                }
                """);
    }

    @Test
    void rejectsBreakAsDirectWhileBody() {
        assertInvalid("""
                while (x < 10)
                    break;
                """);
    }

    @Test
    void rejectsContinueAsDirectWhileBody() {
        assertInvalid("""
                while (x < 10)
                    continue;
                """);
    }

    @Test
    void rejectsSeveralIndependentTopLevelLoops() {
        assertInvalid("""
                while (x < 10) {
                    x++;
                }

                while (y < 10) {
                    y++;
                }
                """);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "while",
            "break",
            "continue",
            "true",
            "false"
    })
    void rejectsReservedWordsAsAssignmentTarget(String keyword) {
        String code = """
                while (x < 10) {
                    %s = 5;
                }
                """.formatted(keyword);

        assertInvalid(code);
    }

    private void assertValid(String code) {
        Parser parser = createParser(code);

        assertDoesNotThrow(parser::parse);
    }

    private void assertInvalid(String code) {
        Parser parser = createParser(code);

        assertThrows(ParserException.class, parser::parse);
    }

    private Parser createParser(String code) {
        Lexer lexer = new Lexer(code);

        return new Parser(lexer.tokenize());
    }
}