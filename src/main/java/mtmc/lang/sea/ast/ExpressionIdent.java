package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionIdent extends Expression {
    public ExpressionIdent(Token token, SeaType type) {
        super(token, token, type);
    }

    public String name() {
        return start.content();
    }
}
