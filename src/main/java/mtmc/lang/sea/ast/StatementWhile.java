package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementWhile extends Statement {
    public final Expression condition;
    public final Statement body;

    public StatementWhile(Token start, Expression condition, Statement body) {
        super(start, body.end);
        this.condition = condition;
        this.body = body;
    }
}
