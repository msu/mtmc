package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class Unit extends Ast {
    public final List<Declaration> declarations;
    public final LinkedHashMap<String, Object> symbols;

    public Unit(ArrayList<Declaration> declarations, LinkedHashMap<String, Object> symbols) {
        super(Token.SOF, Token.EOF);
        this.declarations = declarations;
        this.symbols = symbols;
    }
}
