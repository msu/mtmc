package mtmc.lang.sea.ast;

import mtmc.lang.sea.ParseException;

public final class ExpressionTypeError extends Expression implements Error {
    public final Expression inner;
    public final ParseException exception;

    public ExpressionTypeError(Expression inner, String message) {
        super(inner.start, inner.end);
        this.inner = inner;
        this.exception = new ParseException(inner.span(), message);
    }


    @Override
    public ParseException exception() {
        return null;
    }
}
