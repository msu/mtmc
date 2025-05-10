package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionInteger extends Expression {
    public final int value;

    public ExpressionInteger(Token start) {
        super(start, start);
        this.value = Integer.parseInt(start.content());
    }
}
