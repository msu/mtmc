package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionPrefix extends Expression {
    public final Expression inner;

    public ExpressionPrefix(Token operator, Expression rhs, SeaType type) {
        super(operator, rhs.end, type);
        this.inner = rhs;
    }

    public String op() {
        return start.content();
    }
}
