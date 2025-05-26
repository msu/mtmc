package mtmc.lang.sea.ast;

import mtmc.lang.ParseException;
import mtmc.lang.sea.Token;

public final class StatementSyntaxError extends Statement implements SyntaxError {
    public final ParseException exception;

    public StatementSyntaxError(Token token, ParseException exception) {
        super(token, token);
        this.exception = exception;
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
