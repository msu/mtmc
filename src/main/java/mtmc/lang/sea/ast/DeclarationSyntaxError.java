package mtmc.lang.sea.ast;

import mtmc.lang.ParseException;
import mtmc.lang.sea.Token;

public final class DeclarationSyntaxError extends Declaration implements SyntaxError {
    public final ParseException exception;

    public DeclarationSyntaxError(Token token, ParseException parseException) {
        super(token, token);
        this.exception = parseException;
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
