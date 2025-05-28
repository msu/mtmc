package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;
import mtmc.lang.sea.Token;

public final class StatementSyntaxError extends Statement implements SyntaxError {
    public final ParseException exception;

    public StatementSyntaxError(Token token, String message) {
        super(token, token);
        this.exception = new ParseException(token, message);
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
