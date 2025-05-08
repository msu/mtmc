package mtmc.lang.sea.ast;

public final class ExpressionTernary extends Expression {
    public final Expression cond;
    public final Expression then;
    public final Expression otherwise;

    public ExpressionTernary(Expression cond, Expression then, Expression otherwise) {
        super(cond.start, otherwise.end);
        this.cond = cond;
        this.then = then;
        this.otherwise = otherwise;
    }
}
