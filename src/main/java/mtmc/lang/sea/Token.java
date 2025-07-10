package mtmc.lang.sea;

import mtmc.lang.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Token(
        Token.Type type,
        @NotNull
        String content,
        int start,
        int end
) implements mtmc.lang.Token {
    public static final Token SOF = new Token(Token.Type.SOF, "", 0, 0);
    public static final Token EOF = new Token(Token.Type.EOF, "", Integer.MAX_VALUE, Integer.MAX_VALUE);

    public enum Type {
        // Special
        LIT_INT(null),
        LIT_STR(null),
        LIT_CHAR(null),
        LIT_IDENT(null),
        KW_TYPEDEF("typedef"),
        KW_STRUCT("struct"),
        KW_IF("if"),
        KW_ELSE("else"),
        KW_FOR("for"),
        KW_WHILE("while"),
        KW_DO("do"),
        KW_GOTO("goto"),
        KW_CONTINUE("continue"),
        KW_BREAK("break"),
        KW_RETURN("return"),
        KW_SIZEOF("sizeof"),
        KW_INT("int"),
        KW_CHAR("char"),
        KW_VOID("void"),
        SOF(null),
        EOF(null),

        // Groups
        LEFT_PAREN("("),
        RIGHT_PAREN(")"),
        LEFT_BRACKET("["),
        RIGHT_BRACKET("]"),
        LEFT_BRACE("{"),
        RIGHT_BRACE("}"),

        // Simple Punct
        DOT3("..."),
        DOT("."),
        SEMICOLON(";"),
        COMMA(","),
        COLON(":"),
        TILDE("~"),
        QUESTION("?"),

        PLUS2("++"),
        PLUS_EQ("+="),
        PLUS("+"),

        DASH2("--"),
        DASH_EQ("-="),
        ARROW("->"),
        DASH("-"),

        STAR_EQ("*="),
        STAR("*"),

        SLASH_EQ("/="),
        SLASH("/"),

        PERCENT_EQ("%="),
        PERCENT("%"),

        AMPERSAND2("&&"),
        AMPERSAND_EQ("&="),
        AMPERSAND("&"),

        BAR2("||"),
        BAR_EQ("|="),
        BAR("|"),

        CARET("^"),
        CARET_EQ("^="),

        LEFT_ARROW2_EQ("<<="),
        LEFT_ARROW2("<<"),
        LEFT_ARROW_EQ("<="),
        LEFT_ARROW("<"),

        RIGHT_ARROW2_EQ(">>="),
        RIGHT_ARROW2(">>"),
        RIGHT_ARROW_EQ(">="),
        RIGHT_ARROW(">"),

        EQUAL2("=="),
        EQUAL("="),

        BANG_EQ("!="),
        BANG("!");

        public final String lex;

        public static final Type[] PUNCT;
        static {
            List<Type> list = new ArrayList<>();
            for (Type t : Type.values()) {
                if (t.lex != null) {
                    list.add(t);
                }
            }
            PUNCT = list.toArray(new Type[0]);
        }

        public static final Type[] KEYWORDS;
        static {
            List<Type> list = new ArrayList<>();
            for (Type t : Type.values()) {
                if (t.name().startsWith("KW_")) {
                    list.add(t);
                }
            }
            KEYWORDS = list.toArray(new Type[0]);
        }

        Type(String lex) {
            this.lex = lex;
        }
    }

    public static int[] getLineAndOffset(String src, int index) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < index && i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '\n') {
                line = line + 1;
                column = 1;
            } else {
                column = column + 1;
            }
        }
        return new int[]{line, column};
    }

    public static String getLineFor(String src, int index) {
        int start = 0;
        for (int i = Math.min(index, src.length() - 1); i >= 0; i--) {
            if (src.charAt(i) == '\n') {
                start = i + 1;
                break;
            }
        }

        int end = src.length();
        for (int i = index; i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                break;
            }
            end = i + 1;
        }
        return src.substring(start, end);
    }

    public static String highlight(String src, int start, int end) {
        var s = getLineAndOffset(src, start);
        var e = getLineAndOffset(src, end);

        int lineStart;
        if (s[0] != e[0]) {
            lineStart = 0;
        } else {
            lineStart = s[1] - 1;
        }

        int lineEnd = e[1] - 1;

        String line = getLineFor(src, end);

        StringBuilder result = new StringBuilder();
        int off = 0;

        if (lineStart > 10) {
            result.append("... ");
            off += 4;
            result.append(line.substring(lineStart, lineEnd));
        } else {
            result.append(line.substring(0, lineEnd));
        }

        result.append('\n');
        result.repeat(' ', off + lineStart);
        if (start == Integer.MAX_VALUE) {
            result.append("^ (at EOL)");
        } else {
            result.repeat('^', lineEnd - lineStart);
            result.append(" (here)");
        }
        return result.toString();
    }

    public static List<Token> tokenize(String src) throws TokenizeException {
        List<Token> tokens = new ArrayList<>();
        int offset = 0;
        do {
            Token token = tokenizeOne(src, offset);
            if (token == null) break;
            tokens.add(token);
            offset = token.end();
        } while (true);
        return tokens;
    }

    private static boolean match(String str, int start, String token) {
        if (str == null) return false;
        if (str.length() - start < token.length()) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = str.charAt(start + i);
            char d = token.charAt(i);
            if (c != d) return false;
        }
        return true;
    }

    private static boolean match(String str, int start, char c) {
        if (str == null) return false;
        if (str.length() - start < Character.charCount(c)) return false;
        return str.charAt(start) == c;
    }

    public static Token tokenizeOne(String src, int offset) throws TokenizeException {
        while (offset < src.length()) {
            if (Character.isWhitespace(src.charAt(offset))) {
                offset += Character.charCount(src.charAt(offset));
            } else if (match(src, offset, "//")) {
                offset += 2;
                while (offset < src.length()) {
                    char c = src.charAt(offset);
                    offset += Character.charCount(c);
                    if (c == '\n') {
                        break;
                    }
                }
            } else if (match(src, offset, "/*")) {
                offset += 2;
                while (offset < src.length()) {
                    if (match(src, offset, "*/")) {
                        offset += 2;
                        break;
                    } else {
                        offset += Character.charCount(src.charAt(offset));
                    }
                }
            } else {
                break;
            }
        }
        if (offset >= src.length()) return null;

        int start = offset;
        Type type;
        String content = null;

        char c = src.charAt(offset);
        if (Character.isDigit(c)) {
            do {
                offset += Character.charCount(src.charAt(offset));
            } while (offset < src.length() && Character.isDigit(src.charAt(offset)));
            content = src.substring(start, offset);
            type = Type.LIT_INT;
        } else if (Character.isLetter(c) || c == '_') {
            do {
                offset += Character.charCount(src.charAt(offset));
            } while (offset < src.length() && (Character.isLetter(src.charAt(offset)) || Character.isDigit(src.charAt(offset)) || src.charAt(offset) == '_'));
            content = src.substring(start, offset);
            type = Type.LIT_IDENT;
            for (var ty : Type.KEYWORDS) {
                if (content.equals(ty.lex)) {
                    type = ty;
                    break;
                }
            }
        } else if (c == '\'') {
            offset += Character.charCount(c);
            char d = src.charAt(offset);
            offset += Character.charCount(d);
            if (d == '\\') {
                if (offset >= src.length()) throw new TokenizeException("invalid character escape " + d, start, offset);
                d = src.charAt(offset);
                offset += Character.charCount(d);
                content = switch (d) {
                    case 'n':
                        yield "\n";
                    case 'r':
                        yield "\r";
                    case 't':
                        yield "\t";
                    case '\\':
                        yield "\\";
                    case '\'':
                        yield "'";
                    case '"':
                        yield "\"";
                    case '?':
                        yield "?";
                    default:
                        throw new TokenizeException("invalid character escape " + d, start, offset);
                };
            } else {
                content = String.valueOf(d);
            }

            if (offset >= src.length() || src.charAt(offset) != '\'') {
                throw new TokenizeException("unterminated character literal", start, offset);
            }
            offset += Character.charCount('\'');
            type = Type.LIT_CHAR;
        } else if (c == '"') {
            offset += Character.charCount(src.charAt(offset));
            StringBuilder sb = new StringBuilder();
            while (offset < src.length() && src.charAt(offset) != '"') {
                char d = src.charAt(offset);
                offset += Character.charCount(d);

                if (d == '\\') {
                    d = src.charAt(offset);
                    offset += Character.charCount(d);
                    char s = switch (d) {
                        case 'n':
                            yield '\n';
                        case 'r':
                            yield '\r';
                        case 't':
                            yield '\t';
                        case '\\':
                            yield '\\';
                        case '\'':
                            yield '\'';
                        case '"':
                            yield '"';
                        case '?':
                            yield '?';
                        default:
                            throw new TokenizeException("invalid string escape " + d, start, offset);
                    };
                    sb.append(s);
                } else if (d == '\n') {
                    break;
                } else {
                    sb.append(d);
                }
            }

            if (offset >= src.length() || src.charAt(offset) != '"') {
                throw new TokenizeException("unterminated string literal", start, offset);
            }

            content = sb.toString();
            offset += Character.charCount('\"');
            type = Type.LIT_STR;
        } else {
            type = null;
            for (Type t : Type.PUNCT) {
                if (match(src, start, t.lex)) {
                    type = t;
                    content = t.lex;
                    offset += t.lex.length();
                    break;
                }
            }

            if (type == null) {
                throw new TokenizeException("unexpected character '" + src.charAt(start) + "'", start, offset);
            }
        }

        Objects.requireNonNull(content);
        return new Token(type, content, start, offset);
    }

    public static class TokenizeException extends IllegalArgumentException {
        public final int start, end;

        public TokenizeException(String msg, int start, int end) {
            super(msg);
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "TokenizeException at " + start + ":" + end + ", " + getLocalizedMessage();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String s) return Objects.equals(content, s);
        if (!(o instanceof Token token)) return false;
        return end == token.end && start == token.start && Objects.equals(content, token.content) && type == token.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, content, start, end);
    }

    @Override
    public Location getStart() {
        return new Location(start);
    }

    @Override
    public Location getEnd() {
        return new Location(end);
    }

    @Override
    public String getContent() {
        return content();
    }
}
