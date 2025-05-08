package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementContinue extends Statement {
    public StatementContinue(Token continueToken) {
        super(continueToken, continueToken);
    }
}
