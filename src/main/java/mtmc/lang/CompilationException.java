package mtmc.lang;

public class CompilationException extends Exception {
    protected Span span;

    public CompilationException(String message, Span span) {
        super(message);
        this.span = span;
    }

    public CompilationException(CompilationException parent, String message) {
        super(message, parent);
        this.span = parent.span;
    }

    public Span getSpan() {
        return span;
    }
}
