package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionParens extends Expression {
    public final Expression expression;

    public ExpressionParens(Token start, Expression expression, Token end) {
        super(start, end);
        this.expression = expression;
    }
}
