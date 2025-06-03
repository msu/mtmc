package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionParens extends Expression {
    public final Expression inner;

    public ExpressionParens(Token start, Expression inner, Token end) {
        super(start, end, inner.type());
        this.inner = inner;
    }
}
