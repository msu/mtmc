package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementReturn extends Statement {
    public final Expression value;

    public StatementReturn(Token start, Expression value) {
        super(start, value == null ? start : value.end);
        this.value = value;
    }
}
