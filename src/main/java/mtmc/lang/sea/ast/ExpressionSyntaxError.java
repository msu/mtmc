package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionSyntaxError extends Expression {
    public final String message;

    public ExpressionSyntaxError(Expression parent, Token token, String message) {
        super(parent == null ? token : parent.start, token);
        this.message = message;
    }
}
