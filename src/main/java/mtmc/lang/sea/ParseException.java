package mtmc.lang.sea;

public class ParseException extends Exception {
    public final Token token;

    public ParseException(Token token, String message) {
        super(message);
        this.token = token;
    }

    public ParseException(Token token, String message, Throwable cause) {
        super(message, cause);
        this.token = token;
    }
}
