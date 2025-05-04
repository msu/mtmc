package mtmc.lang.sea.ast;

import mtmc.lang.sea.Token;

public final class StatementSyntaxError extends Statement {
    public final String message;

    public StatementSyntaxError(Token token, String message) {
        super(token, token);
        this.message = message;
    }
}
