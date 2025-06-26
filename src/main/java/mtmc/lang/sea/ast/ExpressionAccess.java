package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionAccess extends Expression {
    public final Expression value;
    public final Token access;
    public final ExpressionIdent ident;

    public ExpressionAccess(Expression value, Token access, ExpressionIdent ident, SeaType type) {
        super(value.start, ident.end, type);
        this.value = value;
        this.access = access;
        this.ident = ident;
    }
}
