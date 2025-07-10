package mtmc.tokenizer;

public record MTMCToken(
        int start,
        int end,
        int line,
        int lineOffset,
        String stringValue,
        TokenType type
) {
    public static MTMCToken join(MTMCToken a, MTMCToken b, TokenType type) {
        if (a.end != b.start) throw new IllegalArgumentException("tokens must be joint!");
        return new MTMCToken(
                a.start,
                b.end,
                a.line,
                a.lineOffset,
                a.stringValue + b.stringValue,
                type
        );
    }

    @Override
    public String toString() {
        return stringValue;
    }

    public String stringValue() {
        return stringValue;
    }

    public char charValue() {
        return stringValue.charAt(0);
    }

    public Integer intValue() {
        if (type == TokenType.INTEGER) {
            return Integer.parseInt(stringValue);
        } else if (type == TokenType.HEX) {
            String stripped = stringValue.substring(2);
            short i = Short.parseShort(stripped, 16);
            return (int) i;
        } else if (type == TokenType.BINARY) {
            String stripped = stringValue.substring(2);
            short i = Short.parseShort(stripped.replaceAll("_", ""), 2);
            return (int) i;
        } else {
            throw new UnsupportedOperationException("Cannot return int for type " + type());
        }
    }

    public String labelValue() {
        return stringValue.substring(0, stringValue.length() - 1);
    }

    public MTMCToken cloneWithVal(String val) {
        return new MTMCToken(start, end, line, lineOffset, val, type);
    }

    public enum TokenType {
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        COLON,
        COMMA,
        DOT,
        MINUS,
        PLUS,
        SLASH,
        AT,
        STAR,
        QUESTION_MARK,
        BANG,
        BANG_EQUAL,
        EQUAL,
        EQUAL_EQUAL,
        GREATER,
        GREATER_EQUAL,
        LESS, LESS_EQUAL,
        IDENTIFIER,
        LABEL,
        STRING,
        CHAR,
        INTEGER,
        DECIMAL,
        HEX,
        BINARY,
        ERROR,
        SOF,
        EOF
    }
}
