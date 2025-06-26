package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;

public final class ExpressionPostfix extends Expression {
    public Expression inner;

    public ExpressionPostfix(Expression lhs, Token op, SeaType type) {
        super(lhs.start, op, type);
        this.inner = lhs;
    }

    public String op() {
        return end.content();
    }
}
