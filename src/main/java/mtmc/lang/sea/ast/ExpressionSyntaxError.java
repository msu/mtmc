package mtmc.lang.sea.ast;

import mtmc.lang.ParseException;
import mtmc.lang.sea.SeaType;
import mtmc.lang.sea.Token;
import org.jetbrains.annotations.Nullable;

public final class ExpressionSyntaxError extends Expression implements SyntaxError {
    @Nullable
    public final Expression child;
    public final ParseException exception;

    public ExpressionSyntaxError(Token token, String message) {
        this(null, token, message);
    }

    public ExpressionSyntaxError(@Nullable Expression child, Token token, String message) {
        super(child == null ? token : child.start, token, SeaType.INT);
        this.child = child;
        this.exception = new ParseException(new ParseException.Message(token, message));
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
