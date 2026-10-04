package ru.tlp;

import ru.tlp.lexer.Lexer;
import ru.tlp.lexer.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String code = """
                while (x <= 10 && flag != false) {
                    x++;
                    continue;
                }
                """;

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}