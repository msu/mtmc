package edu.montana.cs.mtmc.tokenizer;

import java.util.HashMap;
import java.util.Map;

public class MTMCToken {

    int start;
    int end;
    int line;
    int lineOffset;
    String stringValue;
    TokenType type;

    public MTMCToken(TokenType tokenType, String stringValue, int start, int end, int line, int lineOffset) {
        this.start = start;
        this.end = end;
        this.type = tokenType;
        this.stringValue = stringValue;
        this.line = line;
        this.lineOffset = lineOffset;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getLine() {
        return line;
    }

    public int getLineOffset() {
        return lineOffset;
    }

    public String getStringValue() {
        return stringValue;
    }

    public TokenType getType() {
        return type;
    }

    public Integer getIntegerValue() {
        if (type == TokenType.INTEGER) {
            return Integer.parseInt(stringValue);
        } else {
            throw new UnsupportedOperationException("Cannot return int for type " + getType());
        }
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
