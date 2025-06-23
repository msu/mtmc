package mtmc.lang.sea.ast;

import mtmc.lang.sea.SeaType;

public final class ExpressionTernary extends Expression {
    public final Expression cond;
    public final Expression then;
    public final Expression otherwise;

    public ExpressionTernary(Expression cond, Expression then, Expression otherwise, SeaType type) {
        super(cond.start, otherwise.end, type);
        this.cond = cond;
        this.then = then;
        this.otherwise = otherwise;
    }
}
