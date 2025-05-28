package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementBreak extends Statement {
    public StatementBreak(Token breakToken) {
        super(breakToken, breakToken);
    }
}
