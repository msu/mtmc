package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementFor extends Statement {
    public final Expression initExpression;
    public final StatementVar initStatement;
    public final Expression condition;
    public final Expression inc;
    public final Statement body;

    public StatementFor(Token start, Expression initExpression, StatementVar initStatement, Expression condition, Expression inc, Statement body) {
        super(start, body.end);
        this.initExpression = initExpression;
        this.initStatement = initStatement;
        this.condition = condition;
        this.inc = inc;
        this.body = body;
    }
}
