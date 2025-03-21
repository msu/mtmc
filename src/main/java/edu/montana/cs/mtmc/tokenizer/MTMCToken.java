package edu.montana.cs.mtmc.tokenizer;

public record MTMCToken(
        int start,
        int end,
        int line,
        int lineOffset,
        String stringValue,
        TokenType type
) {
    public String stringValue() {
        return stringValue;
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
        INTEGER,
        DECIMAL,
        HEX,
        BINARY,
        ERROR,
        EOF
    }
}
