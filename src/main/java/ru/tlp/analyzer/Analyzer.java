package ru.tlp.analyzer;

import ru.tlp.lexer.Lexer;
import ru.tlp.parser.IronsParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Analyzer {
    public AnalysisResult analyze(String code) {
        List<Diagnostic> errors = new ArrayList<>();
        var tokens = new Lexer(code).tokenize(errors::add);

        errors.addAll(new IronsParser(tokens).parse());

        errors.sort(Comparator.comparingInt(Diagnostic::position));

        if (errors.isEmpty()) {
            return new AnalysisResult(true, "Синтаксис корректен", -1, 0, List.of());
        }

        Diagnostic first = errors.getFirst();
        
        return new AnalysisResult(false,
                errors.stream().map(Diagnostic::message).collect(Collectors.joining("\n")),
                first.position(), first.length(), errors);
    }
}
