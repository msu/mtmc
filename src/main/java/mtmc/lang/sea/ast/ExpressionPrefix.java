package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionPrefix extends Expression {
    public final Expression inner;

    public ExpressionPrefix(Token operator, Expression rhs) {
        super(operator, rhs.end);
        this.inner = rhs;
    }
}
