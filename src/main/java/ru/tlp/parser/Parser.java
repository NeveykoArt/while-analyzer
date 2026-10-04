package ru.tlp.parser;

import ru.tlp.lexer.Token;
import ru.tlp.lexer.TokenType;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int position = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void parse() {
        parseWhile();
        consume(TokenType.EOF);
    }

    // <While> → while ( <Condition> ) <Body>
    private void parseWhile() {
        consume(TokenType.WHILE);
        consume(TokenType.LEFT_PAREN);
        parseCondition();
        consume(TokenType.RIGHT_PAREN);
        parseBody();
    }

    // <Body> → <Block> | <Expression> ; | <While>
    private void parseBody() {
        if (check(TokenType.LEFT_BRACE)) {
            parseBlock();
            return;
        }

        if (check(TokenType.WHILE)) {
            parseWhile();
            return;
        }

        parseExpression();
        consume(TokenType.SEMICOLON);
    }

    // <Block> → { { <Statement> } }
    private void parseBlock() {
        consume(TokenType.LEFT_BRACE);

        while (!check(TokenType.RIGHT_BRACE) && !check(TokenType.EOF)) {
            parseStatement();
        }

        consume(TokenType.RIGHT_BRACE);
    }

    // <Statement> →
    //     <Expression> ;
    //   | <While>
    //   | break ;
    //   | continue ;
    private void parseStatement() {
        if (check(TokenType.WHILE)) {
            parseWhile();
            return;
        }

        if (match(TokenType.BREAK)) {
            consume(TokenType.SEMICOLON);
            return;
        }

        if (match(TokenType.CONTINUE)) {
            consume(TokenType.SEMICOLON);
            return;
        }

        parseExpression();
        consume(TokenType.SEMICOLON);
    }

    // <Condition> → <LogicTerm> { || <LogicTerm> }
    private void parseCondition() {
        parseLogicTerm();

        while (match(TokenType.OR)) {
            parseLogicTerm();
        }
    }

    // <LogicTerm> → <Relation> { && <Relation> }
    private void parseLogicTerm() {
        parseRelation();

        while (match(TokenType.AND)) {
            parseRelation();
        }
    }

    // <Relation> →
    // [ ! ] <Arithmetic>
    // [ (== | != | < | > | <= | >=) <Arithmetic> ]
    private void parseRelation() {
        match(TokenType.NOT);

        parseArithmetic();

        if (match(
                TokenType.EQUAL,
                TokenType.NOT_EQUAL,
                TokenType.LESS,
                TokenType.GREATER,
                TokenType.LESS_EQUAL,
                TokenType.GREATER_EQUAL
        )) {
            parseArithmetic();
        }
    }

    // <Expression> →
    //     <Assignment>
    //   | <Arithmetic>
    //   | ++ <Identifier>
    //   | -- <Identifier>
    //   | <Identifier> ++
    //   | <Identifier> --
    private void parseExpression() {
        if (match(TokenType.INCREMENT, TokenType.DECREMENT)) {
            consume(TokenType.IDENTIFIER);
            return;
        }

        if (check(TokenType.IDENTIFIER)) {
            if (checkNext(
                    TokenType.ASSIGN,
                    TokenType.PLUS_ASSIGN,
                    TokenType.MINUS_ASSIGN,
                    TokenType.STAR_ASSIGN,
                    TokenType.SLASH_ASSIGN,
                    TokenType.PERCENT_ASSIGN
            )) {
                parseAssignment();
                return;
            }

            if (checkNext(TokenType.INCREMENT, TokenType.DECREMENT)) {
                consume(TokenType.IDENTIFIER);
                advance();
                return;
            }
        }

        parseArithmetic();
    }

    // <Assignment> →
    // <Identifier> (= | += | -= | *= | /= | %=) <Arithmetic>
    private void parseAssignment() {
        consume(TokenType.IDENTIFIER);

        if (!match(
                TokenType.ASSIGN,
                TokenType.PLUS_ASSIGN,
                TokenType.MINUS_ASSIGN,
                TokenType.STAR_ASSIGN,
                TokenType.SLASH_ASSIGN,
                TokenType.PERCENT_ASSIGN
        )) {
            throw error("Ожидался оператор присваивания");
        }

        parseArithmetic();
    }

    // <Arithmetic> → <Term> { (+ | -) <Term> }
    private void parseArithmetic() {
        parseTerm();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            parseTerm();
        }
    }

    // <Term> → <Factor> { (* | / | %) <Factor> }
    private void parseTerm() {
        parseFactor();

        while (match(
                TokenType.STAR,
                TokenType.SLASH,
                TokenType.PERCENT
        )) {
            parseFactor();
        }
    }

    // <Factor> →
    //     <Identifier>
    //   | <Number>
    //   | true
    //   | false
    //   | ( <Condition> )
    private void parseFactor() {
        if (match(
                TokenType.IDENTIFIER,
                TokenType.NUMBER,
                TokenType.TRUE,
                TokenType.FALSE
        )) {
            return;
        }

        if (match(TokenType.LEFT_PAREN)) {
            parseCondition();
            consume(TokenType.RIGHT_PAREN);
            return;
        }

        throw error("Ожидался идентификатор, число, true, false или '('");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private void consume(TokenType type) {
        if (check(type)) {
            advance();
            return;
        }

        throw error("Ожидался токен " + type);
    }

    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    private boolean checkNext(TokenType... types) {
        if (position + 1 >= tokens.size()) {
            return false;
        }

        TokenType nextType = tokens.get(position + 1).getType();

        for (TokenType type : types) {
            if (nextType == type) {
                return true;
            }
        }

        return false;
    }

    private Token advance() {
        if (position < tokens.size()) {
            position++;
        }

        return tokens.get(position - 1);
    }

    private Token peek() {
        return tokens.get(position);
    }

    private ParserException error(String message) {
        Token token = peek();

        return new ParserException(
                message
                        + ". Получено: "
                        + token
                        + ", индекс токена: "
                        + position,
                token.getPosition(),
                Math.max(token.getValue().length(), 1)
        );
    }
}
