package mtmc.lang.sea.ast;

import mtmc.lang.sea.Symbol;
import mtmc.lang.sea.Token;

import java.util.LinkedHashMap;
import java.util.List;

public final class Unit extends Ast {
    public final String source;
    public final String filename;
    public final List<Declaration> declarations;
    public final LinkedHashMap<String, Symbol> symbols;

    public Unit(String filename, String source, List<Declaration> declarations, LinkedHashMap<String, Symbol> globals) {
        super(Token.SOF, Token.EOF);
        this.source = source;
        this.filename = filename;
        this.declarations = declarations;
        this.symbols = globals;
    }
}
