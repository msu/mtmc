package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;
import mtmc.lang.sea.Token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class Unit extends Ast {
    public final List<Declaration> declarations;
    public final LinkedHashMap<String, Object> symbols;

    public Unit(List<Declaration> declarations, LinkedHashMap<String, Object> symbols) {
        super(Token.SOF, Token.EOF);
        this.declarations = declarations;
        this.symbols = symbols;
    }

    public List<ParseException> collectErrors() {
        return declarations.stream()
                .flatMap(x -> collectErrors(x).stream())
                .toList();
    }

    public static List<ParseException> collectErrors(Ast ast) {
        if (ast instanceof SyntaxError e) {
            return List.of(e.exception());
        }

        return ast.getChildren()
                .flatMap(x -> collectErrors(x).stream())
                .collect(Collectors.toList());
    }
}
