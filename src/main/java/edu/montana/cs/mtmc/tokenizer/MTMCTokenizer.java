package edu.montana.cs.mtmc.tokenizer;

import edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.stream.Stream;

import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.*;
import static java.util.HexFormat.isHexDigit;

public class MTMCTokenizer {

    // scanning info
    private final String lineCommentStart;
    String src;
    int position = 0;
    int line = 1;
    int lineOffset = 0;

    // tokenization results
    LinkedList<MTMCToken> tokens = new LinkedList<>();
    int currentToken = 0;
    
    public MTMCTokenizer(String source, String lineCommentStart) {
        this.src = source;
        this.lineCommentStart = lineCommentStart;
        tokenize();
    }

    private void tokenize() {
        consumeWhitespace();
        while (!scanEnd()) {
            scanToken();
            consumeWhitespace();
        }
        tokens.add(makeToken(EOF, "<EOF>", position));
    }

    private void scanToken() {
        if (scanLineComment()) {
            return;
        }
        if(scanNumber()) {
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
                } else {
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
        if(isDigit(peek())) {
            if (peek(1) == 'x') {
                return scanHex();
            } else if (peek(1) == 'b') {
                return scanBinary();
            } else {
                return scanDecimal();
            }
        } else {
            return false;
        }
    }

    private boolean scanDecimal() {
        int start = position;
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

    private boolean scanHex() {
        int start = position;
        while (isHexDigit(peek())) {
            takeChar();
        }
        tokens.add(makeToken(HEX, strValueFrom(start), start));
        return true;
    }

    private boolean scanBinary() {
        int start = position;
        while (peek() == '1' || peek() == '0') {
            takeChar();
        }
        tokens.add(makeToken(BINARY, strValueFrom(start), start));
        return true;
    }

    @NotNull
    private MTMCToken makeToken(TokenType decimal, String stringValue, int startPosition) {
        return new MTMCToken(decimal, stringValue, startPosition, position, line, lineOffset);
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
        return src.charAt(position);
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

    // token stream method
    
    public MTMCToken currentToken() {
        return tokens.get(currentToken);
    }

    public MTMCToken consume() {
        return tokens.get(currentToken++);
    }

    public String consumeAsString() {
        return tokens.get(currentToken++).getStringValue();
    }

    public MTMCToken matchAndConsume(TokenType... type) {
        if (match(type)) {
            return consume();
        } else {
            return null;
        }
    }

    public boolean matchAndConsume(String identifier) {
        if (currentToken().getType().equals(IDENTIFIER) &&
                currentToken().getStringValue().equals(identifier)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean match(TokenType... type) {
        for (TokenType tokenType : type) {
            if (currentToken().getType().equals(tokenType)) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        currentToken = 0;
    }

    public boolean more() {
        return !currentToken().getType().equals(EOF);
    }

    public MTMCToken previousToken() {
        return tokens.get(Math.max(0, currentToken - 1));
    }

    public Stream<MTMCToken> stream() {
        return tokens.stream();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            MTMCToken token = tokens.get(i);
            if (i == currentToken) {
                sb.append("-->[");
            }
            sb.append(token.getStringValue());
            if (i == currentToken) {
                sb.append("]<--");
            }
            sb.append(" ");
        }
        return sb.toString();
    }

    // collapse all adjacent tokens into a string
    public String collapseTokensAsString() {
        if (more()) {
            StringBuilder sb = new StringBuilder();
            MTMCToken last = consume();
            MTMCToken next = currentToken();
            while (more() && last.end + 1 == next.start) {
                sb.append(last.getStringValue());
                last = consume();
                next = currentToken();
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public Integer consumeAsInteger() {
        return consume().getIntegerValue();
    }

    public MTMCToken require(TokenType tokenType, Runnable notFound) {
        if (match(tokenType)) {
            return consume();
        } else {
            notFound.run();
            return null;
        }
    }

}