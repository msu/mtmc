package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementIf extends Statement {
    public final Expression condition;
    public final Statement body;
    public final Statement elseBody;

    public StatementIf(Token start, Expression condition, Statement body, Statement elseBody) {
        super(start, elseBody == null ? body.end : elseBody.end);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }
}
