package mtmc.tokenizer;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

import static mtmc.tokenizer.MTMCToken.TokenType.*;
import static mtmc.tokenizer.MTMCToken.TokenType.ERROR;
import static java.util.HexFormat.isHexDigit;

public class MTMCScanner {

    private final String src;
    private final String lineCommentStart;
    int position = 0;
    int line = 1;
    int lineOffset = 0;
    LinkedList<MTMCToken> tokens = new LinkedList<>();

    public MTMCScanner(String source, String lineCommentStart) {
        this.src = source;
        this.lineCommentStart = lineCommentStart;
    }

    public LinkedList<MTMCToken> tokenize() {
        consumeWhitespace();
        while (!scanEnd()) {
            scanToken();
            consumeWhitespace();
        }
        tokens.add(makeToken(EOF, "<EOF>", position));
        return tokens;
    }

    private void scanToken() {
        if (scanLineComment()) {
            return;
        }
        if(scanNumber()) {
            return;
        }
        if (scanChar()) {
            return;
        }
        if(scanString()) {
            return;
        }
        if(scanIdentifier()) {
            return;
        }
        scanSyntax();
    }

    private boolean scanLineComment() {
        byte[] bytes = lineCommentStart.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            byte aChar = bytes[i];
            if (peek(i) != aChar) {
                return false;
            }
        }
        // consume line comment
        while (peek() != '\n' && moreChars()) {
            takeChar();
        }
        return true;
    }

    private boolean moreChars() {
        return !scanEnd();
    }

    private boolean scanChar() {
        StringBuilder sb = new StringBuilder();
        if (peek() != '\'') return false;

        int start = position;
        takeChar();
        char c;
        if (scanEnd()) {
            tokens.add(makeToken(ERROR, strValueFrom(start + 1), start));
            return true;
        }
        c = takeChar();
        if (c == '\\') {
            if (moreChars()) {
                char nextChar = takeChar();
                if (nextChar == 'n') {
                    sb.append('\n');
                } else if (nextChar == 't') {
                    sb.append('\t');
                } else {
                    sb.append(nextChar);
                }
            }
        } else {
            sb.append(c);
        }
        if (scanEnd()) {
            tokens.add(makeToken(ERROR, strValueFrom(start + 1), start));
            return true;
        }
        c = takeChar();
        if (c != '\'') {
            tokens.add(makeToken(ERROR, strValueFrom(start + 1), start));
            return true;
        }

        tokens.add(makeToken(CHAR, sb.toString(), start));
        return true;
    }

    private boolean scanString() {
        StringBuilder sb = new StringBuilder();
        if(peek() == '"') {
            int start = position;
            takeChar();
            char c;
            do {
                if (scanEnd()) {
                    tokens.add(makeToken(ERROR, strValueFrom(start + 1), start));
                    return true;
                }
                c = takeChar();
                if (c == '\\') {
                    if (moreChars()) {
                        char nextChar = takeChar();
                        if (nextChar == 'n') {
                            sb.append('\n');
                        } else if (nextChar == 't') {
                            sb.append('\t');
                        } else {
                            sb.append(nextChar);
                        }
                    }
                } else if(c != '"') {
                    sb.append(c);
                }
            } while (c != '"');
            tokens.add(makeToken(STRING, sb.toString(), start));
            return true;
        } else {
            return false;
        }
    }

    private boolean scanIdentifier() {
        if(isAlpha(peek())) {
            int start = position;
            while (isAlphaNumeric(peek())) {
                takeChar();
            }
            if (peek() == ':') {
                takeChar();
                tokens.add(makeToken(LABEL, strValueFrom(start), start));
            } else {
                tokens.add(makeToken(IDENTIFIER, strValueFrom(start), start));
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean scanNumber() {
        if(isDigit(peek()) || (peek() == '-' && isDigit(peek(1)))) {
            boolean negative = peek() == '-';
            int start = position;
            if (negative) {
                takeChar();
            } else if (peek() == '0') {
                if (peek(1) == 'x') {
                    return scanHex(start);
                }
                if (peek(1) == 'b') {
                    return scanBinary(start);
                }
            }

            return scanDecimal(start);
        } else {
            return false;
        }
    }

    private boolean scanDecimal(int start) {
        while (isDigit(peek())) {
            takeChar();
        }
        if (peek() == '.') {
            //decimal
            while (isDigit(peek())) {
                takeChar();
            }
            tokens.add(makeToken(DECIMAL, strValueFrom(start), start));
        } else {
            tokens.add(makeToken(INTEGER, strValueFrom(start), start));
        }
        return true;
    }

    private boolean scanHex(int start) {
        takeChar(); // take leading zero
        takeChar(); // take 'x'
        while (isHexDigit(peek())) {
            takeChar();
        }
        tokens.add(makeToken(HEX, strValueFrom(start), start));
        return true;
    }

    private boolean scanBinary(int start) {
        takeChar(); // take leading zero
        takeChar(); // take 'b'
        while (peek() == '1' || peek() == '0' || peek() == '_') {
            takeChar();
        }
        tokens.add(makeToken(BINARY, strValueFrom(start), start));
        return true;
    }

    @NotNull
    private MTMCToken makeToken(MTMCToken.TokenType type, String stringValue, int startPosition) {
        return new MTMCToken(startPosition, position, line, lineOffset, stringValue, type);
    }

    @NotNull
    private String strValueFrom(int start) {
        return src.substring(start, position);
    }

    private void scanSyntax() {
        int start = position;
        if(consumeIf('(')) {
            tokens.add(makeToken(LEFT_PAREN, "(", start));
        } else if(consumeIf(')')) {
            tokens.add(makeToken(RIGHT_PAREN, ")", start));
        } else if(consumeIf('?')) {
            tokens.add(makeToken(QUESTION_MARK, "?", start));
        } else if(consumeIf('[')) {
            tokens.add(makeToken(LEFT_BRACKET, "[", start));
        } else if(consumeIf(']')) {
            tokens.add(makeToken(RIGHT_BRACKET, "]", start));
        } else if(consumeIf('{')) {
            tokens.add(makeToken(LEFT_BRACE, "{", start));
        } else if(consumeIf('}')) {
            tokens.add(makeToken(RIGHT_BRACE, "}", start));
        } else if(consumeIf(':')) {
            tokens.add(makeToken(COLON, ":", start));
        } else if(consumeIf(',')) {
            tokens.add(makeToken(COMMA, ",", start));
        } else if(consumeIf('.')) {
            tokens.add(makeToken(DOT, ".", start));
        } else if(consumeIf('+')) {
            tokens.add(makeToken(PLUS, "+", start));
        } else if(consumeIf('-')) {
            tokens.add(makeToken(MINUS, "-", start));
        } else if(consumeIf('*')) {
            tokens.add(makeToken(STAR, "*", start));
        } else if(consumeIf('/')) {
            tokens.add(makeToken(SLASH, "/", start));
        } else if(consumeIf('@')) {
            tokens.add(makeToken(AT, "@", start));
        } else if(consumeIf('!')) {
            if (consumeIf('=')) {
                tokens.add(makeToken(BANG_EQUAL, "!=", start));
            } else {
                tokens.add(makeToken(BANG, "!", start));
            }

        } else if(consumeIf('=')) {
            if (consumeIf('=')) {
                tokens.add(makeToken(EQUAL_EQUAL, "==", start));
            } else {
                tokens.add(makeToken(EQUAL, "=", start));
            }
        } else if(consumeIf('<')) {
            if (consumeIf('=')) {
                tokens.add(makeToken(LESS_EQUAL, "<=", start));
            } else {
                tokens.add(makeToken(LESS, "<", start));
            }
        } else if(consumeIf('>')) {
            if (consumeIf('=')) {
                tokens.add(makeToken(GREATER_EQUAL, ">=", start));
            } else {
                tokens.add(makeToken(GREATER, ">", start));
            }
        } else {
            tokens.add(makeToken(ERROR, "<Unexpected Token: [" + takeChar() + "]>", start));
        }
    }

    private void consumeWhitespace() {
        while (!scanEnd()) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t') {
                position++;
                lineOffset++;
                continue;
            } else if (c == '\n') {
                position++;
                line++;
                lineOffset = 0;
                continue;
            }
            break;
        }
    }

    //===============================================================
    // Utility functions
    //===============================================================

    private char peek() {
        return peek(0);
    }

    private char peek(int offset) {
        return charAt(position + offset);
    }

    private char charAt(int index) {
        if (0 > index || src.length() <= index) return '\0';
        return src.charAt(index);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char takeChar() {
        char c = src.charAt(position);
        position++;
        lineOffset++;
        return c;
    }

    private boolean scanEnd() {
        return position >= src.length();
    }

    private boolean consumeIf(char c) {
        if (peek() == c) {
            takeChar();
            return true;
        }
        return false;
    }
    @Override
    public String toString() {
        if (scanEnd()) {
            return src + "-->[]<--";
        } else {
            return src.substring(0, position) + "-->[" + peek() + "]<--" +
                    ((position == src.length() - 1) ? "" :
                            src.substring(position + 1, src.length() - 1));
        }
    }

}
