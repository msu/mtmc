package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementDoWhile extends Statement {
    public final Statement body;
    public final Expression condition;

    public StatementDoWhile(Token start, Statement body, Expression condition, Token end) {
        super(start, end);
        this.body = body;
        this.condition = condition;
    }
}
