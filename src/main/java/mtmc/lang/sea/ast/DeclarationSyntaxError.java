package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;
import mtmc.lang.sea.Token;

public final class DeclarationSyntaxError extends Declaration implements SyntaxError {
    public final ParseException exception;

    public DeclarationSyntaxError(Token token, String message) {
        super(token, token);
        this.exception = new ParseException(token, message);
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
