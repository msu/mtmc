package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionPostfix extends Expression {
    public Expression inner;

    public ExpressionPostfix(Expression lhs, Token op) {
        super(lhs.start, op);
        this.inner = lhs;
    }
}
