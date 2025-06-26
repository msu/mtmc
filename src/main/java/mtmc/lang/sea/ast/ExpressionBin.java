package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionBin extends Expression {
    public final Expression lhs;
    public final Token op;
    public final Expression rhs;

    public ExpressionBin(Expression lhs, Token op, Expression rhs, SeaType type) {
        super(lhs.start, rhs.end, type);
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    public String op() {
        return op.content();
    }
}
