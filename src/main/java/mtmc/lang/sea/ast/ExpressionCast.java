package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class ExpressionCast extends Expression {
    public final TypeExpr type;
    public final Expression value;

    public ExpressionCast(Token start, TypeExpr type, Expression value) {
        super(start, value.end, type.type());
        this.type = type;
        this.value = value;
    }
}
