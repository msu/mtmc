package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionAccess extends Expression {
    public final Expression value;
    public final Token access;
    public final Token ident;

    public ExpressionAccess(Expression value, Token access, Token ident) {
        super(value.start, ident);
        this.value = value;
        this.access = access;
        this.ident = ident;
    }
}
