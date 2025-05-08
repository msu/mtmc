package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementGoto extends Statement {
    public final Token label;

    public StatementGoto(Token start, Token label) {
        super(start, label);
        this.label = label;
    }
}
