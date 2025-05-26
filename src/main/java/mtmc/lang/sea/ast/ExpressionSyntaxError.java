package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;
import mtmc.lang.sea.Span;
import mtmc.lang.sea.Token;
import org.jetbrains.annotations.Nullable;

public final class ExpressionSyntaxError extends Expression implements SyntaxError {
    @Nullable
    public final Expression child;
    public final ParseException exception;

    public ExpressionSyntaxError(Expression child, String message) {
        super(child.start, child.end);
        this.child = child;
        this.exception = new ParseException(child.span(), message);
    }

    public ExpressionSyntaxError(Token token, String message) {
        super(token, token);
        this.child = null;
        this.exception = new ParseException(new Span(token.start(), token.end()), message);
    }

    @Override
    public ParseException exception() {
        return exception;
    }
}
