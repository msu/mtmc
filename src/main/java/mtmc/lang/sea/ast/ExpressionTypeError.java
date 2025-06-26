package mtmc.lang.sea.ast;

import mtmc.lang.ParseException;

public final class ExpressionTypeError extends Expression implements Error {
    public final Expression inner;
    public final ParseException exception;

    public ExpressionTypeError(Expression inner, String message) {
        super(inner.start, inner.end, inner.type());
        this.inner = inner;
        this.exception = new ParseException(new ParseException.Message(inner.span(), message));
    }


    @Override
    public ParseException exception() {
        return exception;
    }
}
