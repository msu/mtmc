package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionInteger extends Expression {
    public final int value;

    public ExpressionInteger(Token token) {
        super(token, token);
        this.value = Integer.parseInt(token.content());
    }
}
