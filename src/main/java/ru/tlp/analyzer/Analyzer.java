package ru.tlp.analyzer;

import ru.tlp.lexer.Lexer;
import ru.tlp.parser.Parser;
import ru.tlp.parser.ParserException;

public class Analyzer {

    public AnalysisResult analyze(String code) {
        try {
            Lexer lexer = new Lexer(code);
            Parser parser = new Parser(lexer.tokenize());

            parser.parse();

            return new AnalysisResult(
                    true,
                    "Синтаксис корректен"
            );

        } catch (ParserException | IllegalArgumentException exception) {
            return new AnalysisResult(
                    false,
                    exception.getMessage()
            );
        }
    }
}