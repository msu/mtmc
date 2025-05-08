package mtmc.lang.sea.ast;

public final class StatementExpression extends Statement {
    public final Expression expression;

    public StatementExpression(Expression expression) {
        super(expression.start, expression.end);
        this.expression = expression;
    }
}
