package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionIdent extends Expression {
    public ExpressionIdent(Token start) {
        super(start, start);
    }

    public String name() {
        return start.content();
    }
}
