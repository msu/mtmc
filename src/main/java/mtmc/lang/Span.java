package mtmc.lang;

public record Span(Token start, Token end) {

    public static Span of(Token token) {
        return new Span(token, token);
    }

    public static Span of(Token start, Token end) {
        return new Span(start, end);
    }

    public boolean isOnSingleLine(String source) {
        int[] lines = Location.getLineNos(source, start.start(), end.end());
        return lines[0] == lines[1];
    }
}
