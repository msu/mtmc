package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;
import mtmc.lang.sea.Token;
import org.jetbrains.annotations.Nullable;

public final class ExpressionSyntaxError extends Expression implements SyntaxError {
    @Nullable
    public final Expression child;
    public final ParseException exception;

    public ExpressionSyntaxError(@Nullable Expression child, Token token, String message) {
        super(child == null ? token : child.start, token);
        this.child = child;
        this.exception = new ParseException(token, message);
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
