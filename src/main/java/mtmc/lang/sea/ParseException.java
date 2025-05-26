package mtmc.lang.sea;

public class ParseException extends Exception {
    public final Span span;

    public ParseException(Span span, String message) {
        super(message);
        this.span = span;
    }
}
