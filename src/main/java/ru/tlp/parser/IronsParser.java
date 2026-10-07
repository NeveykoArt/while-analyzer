package ru.tlp.parser;

import ru.tlp.lexer.Token;
import ru.tlp.lexer.TokenType;
import ru.tlp.analyzer.Diagnostic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.tlp.lexer.TokenType.*;
import static ru.tlp.parser.IronsParser.Rule.*;

public final class IronsParser {
    private sealed interface Symbol permits Rule, Terminal {}

    enum Rule implements Symbol {
        Loop, Body, Block, Statements, Statement, Condition, OrTail, LogicTerm, AndTail,
        Relation, CompareTail, Comparison, Expression, Assignment, AssignOp,
        Arithmetic, AddTail, AddOp, Term, MultiplyTail, MultiplyOp, Factor
    }

    private record Terminal(TokenType type) implements Symbol {}

    private record Pending(Symbol symbol, Rule owner) {}

    record Recovery(List<String> residualSymbols, Set<TokenType> admissibleTokens,
                    List<TokenType> completion, String resumedBranch, int start, int end) {}

    private static final Grammar GRAMMAR = new Grammar();
    private final List<Token> tokens;
    private final List<Pending> pending = new ArrayList<>();
    private final List<Diagnostic> errors = new ArrayList<>();
    private final List<Recovery> recoveries = new ArrayList<>();
    private int position;

    public IronsParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Recovery> recoveries() {
        return List.copyOf(recoveries);
    }

    public List<Diagnostic> parse() {
        pending.add(new Pending(t(EOF), null));
        pending.add(new Pending(Loop, null));
        while (!pending.isEmpty()) {
            Pending task = pending.getLast();
            if (task.symbol() instanceof Terminal terminal) {
                if (peek().getType() == terminal.type()) {
                    pending.removeLast();
                    if (terminal.type() != EOF) {
                        position++;
                    }
                } else {
                    recover();
                }
            } else {
                Rule rule = (Rule) task.symbol();
                List<Symbol> production = choose(rule);
                if (production == null) {
                    recover();
                } else {
                    pending.removeLast();
                    for (int i = production.size() - 1; i >= 0; i--) {
                        pending.add(new Pending(production.get(i), rule));
                    }
                }
            }
        }
        return List.copyOf(errors);
    }

    private List<Symbol> choose(Rule rule) {
        TokenType current = peek().getType();
        List<List<Symbol>> alternatives = GRAMMAR.productions.get(rule);
        if (rule == Loop && current != WHILE && current != LEFT_PAREN) {
            return null;
        }
        if (rule == Expression) {
            if (current == INCREMENT || current == DECREMENT) {
                return alternatives.get(current == INCREMENT ? 2 : 3);
            }
            if (current == IDENTIFIER && position + 1 < tokens.size()) {
                TokenType next = tokens.get(position + 1).getType();
                if (GRAMMAR.first.get(AssignOp).contains(next)) {
                    return alternatives.get(0);
                }
                if (next == INCREMENT || next == DECREMENT) {
                    return alternatives.get(next == INCREMENT ? 4 : 5);
                }
            }
            return alternatives.get(1);
        }
        if (rule == Body) {
            return alternatives.get(current == LEFT_BRACE ? 0 : 1);
        }
        if (rule == Relation) {
            return alternatives.get(current == NOT ? 0 : 1);
        }
        if (alternatives.size() == 1) {
            return alternatives.getFirst();
        }
        for (List<Symbol> alternative : alternatives) {
            if (!alternative.isEmpty() && GRAMMAR.first(alternative).contains(current)) {
                return alternative;
            }
        }
        if (GRAMMAR.nullable.contains(rule) && continuationFirst().contains(current)) {
            return List.of();
        }
        return null;
    }

    private Set<TokenType> continuationFirst() {
        List<Symbol> continuation = new ArrayList<>();
        for (int i = pending.size() - 2; i >= 0; i--) {
            continuation.add(pending.get(i).symbol());
        }
        return GRAMMAR.first(continuation);
    }

    private void recover() {
        int badTokenIndex = position;
        Token badToken = peek();
        List<String> residual = new ArrayList<>();
        Set<TokenType> admissible = EnumSet.noneOf(TokenType.class);
        for (int i = pending.size() - 1; i >= 0; i--) {
            Symbol symbol = pending.get(i).symbol();
            residual.add(symbol.toString());
            admissible.addAll(GRAMMAR.first(List.of(symbol)));
        }
        int target;
        while ((target = resumeIndex(peek().getType())) < 0) {
            position++;
        }
        boolean discarded = position > badTokenIndex;
        int start = discarded ? badToken.getPosition() : insertionPosition();
        int end = discarded ? tokens.get(position - 1).getPosition() + tokens.get(position - 1).getValue().length() : start;
        List<TokenType> completion = new ArrayList<>();
        Pending failed = pending.getLast();
        while (pending.size() - 1 > target) {
            Pending abandoned = pending.removeLast();
            List<TokenType> chain = GRAMMAR.shortest(abandoned.symbol());
            completion.addAll(chain);
            if (!discarded && !chain.isEmpty()) {
                errors.add(error(abandoned.symbol(), badToken, badTokenIndex, start, 0));
            }
        }
        if (discarded) {
            errors.add(error(failed.symbol(), badToken, badTokenIndex, start, end - start));
        }
        Pending resumed = pending.getLast();
        recoveries.add(new Recovery(List.copyOf(residual), Set.copyOf(admissible),
                List.copyOf(completion), String.valueOf(resumed.owner()), start, end));
    }

    private int resumeIndex(TokenType token) {
        boolean structuralBoundary = token == EOF || token == LEFT_BRACE
                || token == RIGHT_BRACE || token == SEMICOLON;
        for (int i = pending.size() - 1; i >= 0; i--) {
            if (GRAMMAR.first(List.of(pending.get(i).symbol())).contains(token)) {
                return i;
            }
            if (!structuralBoundary && pending.get(i).symbol() instanceof Terminal terminal
                    && terminal.type() == RIGHT_PAREN) {
                return -1;
            }
        }
        return -1;
    }

    private int insertionPosition() {
        if (peek().getType() == EOF) {
            return peek().getPosition();
        }
        if (position == 0) {
            return 0;
        }
        Token previous = tokens.get(position - 1);
        return previous.getPosition() + previous.getValue().length();
    }

    private Diagnostic error(Symbol expected, Token received, int index, int start, int length) {
        String message;
        if (expected == Factor) {
            message = "Ожидался идентификатор, число, true, false или '('";
        } else if (expected instanceof Terminal terminal) {
            message = "Ожидался токен " + terminal.type();
        } else {
            message = "Ожидался токен " + GRAMMAR.first(List.of(expected));
        }
        return new Diagnostic(message + ". Получено: " + received + ", индекс токена: " + index,
                start, length);
    }

    private Token peek() {
        return tokens.get(position);
    }

    private static Terminal t(TokenType type) {
        return new Terminal(type);
    }

    private static final class Grammar {
        final Map<Rule, List<List<Symbol>>> productions = new EnumMap<>(Rule.class);
        final Map<Rule, Set<TokenType>> first = new EnumMap<>(Rule.class);
        final Set<Rule> nullable = EnumSet.noneOf(Rule.class);
        final Map<Rule, List<TokenType>> shortest = new EnumMap<>(Rule.class);

        Grammar() {
            p(Loop, t(WHILE), t(LEFT_PAREN), Condition, t(RIGHT_PAREN), Body);
            p(Body, Block); p(Body, Statement);
            p(Block, t(LEFT_BRACE), Statements, t(RIGHT_BRACE));
            p(Statements, Statement, Statements); p(Statements);
            p(Statement, Loop); p(Statement, t(BREAK), t(SEMICOLON));
            p(Statement, t(CONTINUE), t(SEMICOLON)); p(Statement, Expression, t(SEMICOLON));
            p(Condition, LogicTerm, OrTail); p(OrTail, t(OR), LogicTerm, OrTail); p(OrTail);
            p(LogicTerm, Relation, AndTail); p(AndTail, t(AND), Relation, AndTail); p(AndTail);
            p(Relation, t(NOT), Arithmetic, CompareTail); p(Relation, Arithmetic, CompareTail);
            p(CompareTail, Comparison, Arithmetic); p(CompareTail);
            for (TokenType type : List.of(EQUAL, NOT_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL)) {
                p(Comparison, t(type));
            }
            p(Expression, Assignment); p(Expression, Arithmetic);
            p(Expression, t(INCREMENT), t(IDENTIFIER)); p(Expression, t(DECREMENT), t(IDENTIFIER));
            p(Expression, t(IDENTIFIER), t(INCREMENT)); p(Expression, t(IDENTIFIER), t(DECREMENT));
            p(Assignment, t(IDENTIFIER), AssignOp, Arithmetic);
            for (TokenType type : List.of(ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN, PERCENT_ASSIGN)) {
                p(AssignOp, t(type));
            }
            p(Arithmetic, Term, AddTail); p(AddTail, AddOp, Term, AddTail); p(AddTail);
            p(AddOp, t(PLUS)); p(AddOp, t(MINUS));
            p(Term, Factor, MultiplyTail); p(MultiplyTail, MultiplyOp, Factor, MultiplyTail); p(MultiplyTail);
            p(MultiplyOp, t(STAR)); p(MultiplyOp, t(SLASH)); p(MultiplyOp, t(PERCENT));
            for (TokenType type : List.of(IDENTIFIER, NUMBER, TRUE, FALSE)) {
                p(Factor, t(type));
            }
            p(Factor, t(LEFT_PAREN), Condition, t(RIGHT_PAREN));
            for (Rule rule : Rule.values()) {
                first.put(rule, EnumSet.noneOf(TokenType.class));
            }
            computeSets();
        }

        private void p(Rule rule, Symbol... symbols) {
            productions.computeIfAbsent(rule, ignored -> new ArrayList<>()).add(List.of(symbols));
        }

        private void computeSets() {
            boolean changed;
            do {
                changed = false;
                for (Rule rule : Rule.values()) {
                    for (List<Symbol> production : productions.get(rule)) {
                        changed |= first.get(rule).addAll(first(production));
                        if (production.stream().allMatch(this::isNullable)) {
                            changed |= nullable.add(rule);
                        }
                        List<TokenType> chain = new ArrayList<>();
                        boolean known = true;
                        for (Symbol symbol : production) {
                            if (symbol instanceof Rule child && !shortest.containsKey(child)) {
                                known = false;
                                break;
                            }
                            chain.addAll(shortest(symbol));
                        }
                        if (known && (!shortest.containsKey(rule) || chain.size() < shortest.get(rule).size())) {
                            shortest.put(rule, List.copyOf(chain));
                            changed = true;
                        }
                    }
                }
            } while (changed);
        }

        Set<TokenType> first(List<Symbol> sequence) {
            Set<TokenType> result = EnumSet.noneOf(TokenType.class);
            for (Symbol symbol : sequence) {
                if (symbol instanceof Terminal terminal) {
                    result.add(terminal.type());
                } else {
                    result.addAll(first.get((Rule) symbol));
                }
                if (!isNullable(symbol)) {
                    break;
                }
            }
            return result;
        }

        private boolean isNullable(Symbol symbol) {
            return symbol instanceof Rule rule && nullable.contains(rule);
        }

        List<TokenType> shortest(Symbol symbol) {
            return symbol instanceof Terminal terminal ? List.of(terminal.type()) : shortest.get((Rule) symbol);
        }
    }
}
