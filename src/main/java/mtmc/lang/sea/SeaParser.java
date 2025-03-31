package mtmc.lang.sea;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mtmc.lang.sea.Token.Type.*;

public class SeaParser {
    protected List<Token> tokens;
    protected int index = 0;

    public SeaParser(@NotNull List<Token> tokens) {
        this.tokens = tokens;
    }

    public Ast.Expr parseExpr() {
        return parseAssignExpr();
    }

    public Ast.Expr parseAssignExpr() {
        var expr = parseTernaryExpr();
        if (expr == null) return null;

        if (match(Equal, PlusEq, DashEq, StarEq, SlashEq, PercentEq, AmpersandEq, BarEq, CaretEq, LeftArrow2Eq, RightArrow2Eq)) {
            var op = consume();
            var inner = parseAssignExpr();
            return new Ast.BinaryExpr(expr, op, inner);
        } else {
            return expr;
        }
    }

    public Ast.Expr parseTernaryExpr() {
        var expr = parseOrExpr();
        if (expr == null) return null;

        if (take(Question)) {
            var then = parseTernaryExpr();
            if (then == null) {
                then = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after ternary '?'"));
                consumeTo(Colon);
            }
            if (!take(Colon)) {
                return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected ':' in ternary expression"));
            }
            var elseExpr = parseTernaryExpr();
            if (elseExpr == null) {
                elseExpr = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after ternary ':'"));
            }
            return new Ast.TernaryExpr(expr, then, elseExpr);
        } else {
            return expr;
        }
    }

    public Ast.Expr parseOrExpr() {
        var expr = parseAndExpr();
        if (expr == null) return null;

        while (match(Bar2)) {
            var op = consume();
            var rhs = parseAndExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '||'"));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseAndExpr() {
        var expr = parseBinaryOrExpr();
        if (expr == null) return null;

        while (match(Ampersand2)) {
            var op = consume();
            var rhs = parseBinaryOrExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '&&'"));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseBinaryOrExpr() {
        var expr = parseBinaryXorExpr();
        if (expr == null) return null;

        while (match(Bar)) {
            var op = consume();
            var rhs = parseBinaryXorExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '|'"));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseBinaryXorExpr() {
        var expr = parseBinaryAndExpr();
        if (expr == null) return null;

        while (match(Caret)) {
            var op = consume();
            var rhs = parseBinaryAndExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '^'"));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseBinaryAndExpr() {
        var expr = parseEqualityExpr();
        if (expr == null) return null;

        while (match(Ampersand)) {
            var op = consume();
            var rhs = parseEqualityExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '&'"));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseEqualityExpr() {
        var expr = parseOrdinalExpr();
        if (expr == null) return null;

        while (match(Equal2, BangEq)) {
            var op = consume();
            var rhs = parseOrdinalExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '%s'".formatted(op.content())));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseOrdinalExpr() {
        var expr = parseShiftExpr();
        if (expr == null) return null;

        while (match(LeftArrow, LeftArrowEq, RightArrow, RightArrowEq)) {
            var op = consume();
            var rhs = parseShiftExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '%s'".formatted(op.content())));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseShiftExpr() {
        var expr = parseAdditiveExpr();
        if (expr == null) return null;

        while (match(LeftArrow2, RightArrow2)) {
            var op = consume();
            var rhs = parseAdditiveExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '%s'".formatted(op.content())));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseAdditiveExpr() {
        var expr = parseMultiplicativeExpr();
        if (expr == null) return null;

        while (match(Plus, Dash)) {
            var op = consume();
            var rhs = parseMultiplicativeExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '%s'".formatted(op.content())));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseMultiplicativeExpr() {
        var expr = parseCastExpr();
        if (expr == null) return null;

        while (match(Star, Slash, Percent)) {
            var op = consume();
            var rhs = parseCastExpr();
            if (rhs == null) {
                rhs = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected expression after '%s'".formatted(op.content())));
            }
            expr = new Ast.BinaryExpr(expr, op, rhs);
        }

        return expr;
    }

    public Ast.Expr parseCastExpr() {
        return parsePrefixExpr();
    }

    public Ast.Expr parsePrefixExpr() {
        if (match(Plus2, Dash2, Ampersand, Plus, Dash, Tilde, Bang, Sizeof)) {
            var op = consume();
            var inner = parsePrefixExpr();
            if (inner == null) {
                inner = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(),
                        "expected expression after prefix operator '%s'".formatted(op.content())));
            }
            return new Ast.PrefixExpr(op, inner);
        } else {
            return parsePostfixExpr();
        }
    }

    public Ast.Expr parsePostfixExpr() {
        var expr = parsePrimaryExpr();
        while (hasMoreTokens()) {
            if (take(LeftBracket)) {
                var index = parseExpr();
                if (index == null) {
                    index = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected index after '['"));
                    consumeTo(RightBracket);
                }

                if (!match(RightBracket)) {
                    return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected "));
                }
                var rbracket = consume();

                expr = new Ast.IndexExpr(expr, index, rbracket);
            } else if (take(LeftParen)) {
                var args = new ArrayList<Ast.Expr>();
                while (hasMoreTokens() && !match(RightParen)) {
                    var arg = parseExpr();
                    if (arg == null) {
                        arg = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected function argument"));
                        consumeTo(Comma, RightParen);
                    }
                    args.add(arg);
                    if (!take(Comma)) break;
                }

                if (!match(RightParen)) {
                    if (parseExpr() != null) {
                        return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected ')' after function arguments, did you forget a comma?"));
                    }

                    return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected ')' after function arguments"));
                }

                var rparen = consume();
                expr = new Ast.InvokeExpr(expr, args, rparen);
            } else if (take(Dot)) {
                if (!match(Ident)) {
                    return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected field name after '.'"));
                }
                var field = consume();
                expr = new Ast.AccessExpr(expr, new Ast.Ident(field));
            } else if (take(Arrow)) {
                if (!match(Ident)) {
                    return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected field name after '->'"));
                }
                var field = consume();
                expr = new Ast.PtrAccessExpr(expr, new Ast.Ident(field));
            } else if (match(Plus2, Dash2)) {
                var op = consume();
                expr = new Ast.PostfixExpr(expr, op);
            } else {
                break;
            }
        }
        return expr;
    }

    public Ast.Expr parsePrimaryExpr() {
        if (match(Int)) {
            return new Ast.Int(consume());
        } else if (match(Str)) {
            return new Ast.Str(consume());
        } else if (match(Char)) {
            return new Ast.Char(consume());
        } else if (match(Ident)) {
            return new Ast.Ident(consume());
        } else if (match(LeftParen)) {
            var start = consume();
            var inner = parseExpr();
            var end = consume();
            if (end.type() != RightParen) {
                return new Ast.Error(start, end, Map.of(end, "expected ')' in group"));
            }
            return new Ast.Group(start, inner, end);
        } else if (match(LeftBrace)) {
            var start = consume();
            List<Ast.Initializer> initializers = new ArrayList<>();
            while (hasMoreTokens() && !match(RightBrace)) {
                if (match(Dot)) {
                    var dot = consume();
                    if (!match(Ident)) {
                        return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected field name after '.' in initializer list"));
                    }
                    var name = new Ast.Ident(consume());
                    if (!take(Equal)) {
                        return new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected '=' after field-name in initializer list"));
                    }
                    var value = parseExpr();
                    if (value == null) {
                        value = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected value after '=' in initializer list"));
                        consumeTo(Comma, RightBrace);
                    }
                    var init = new Ast.FieldInitializer(dot, name, value);
                    initializers.add(init);
                } else {
                    var value = parseExpr();
                    if (value == null) {
                        value = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected value in initializer list"));
                        consumeTo(Comma, RightBrace);
                    }
                    var init = new Ast.ValueInitializer(value);
                    initializers.add(init);
                }

                if (!take(Comma)) break;
            }

            if (!match(RightBrace)) {
                var error = new Ast.Error(lastToken(), peekToken(), Map.of(peekToken(), "expected '}' after initializer list"));
                return error;
            }

            var end = consume();
            return new Ast.InitializerList(start, initializers, end);
        } else {
            return null;
        }
    }

    protected boolean hasMoreTokens() {
        return index < tokens.size();
    }

    protected boolean match(@NotNull Token.Type... types) {
        for (var type : types) {
            if (peekToken().type().equals(type)) return true;
        }
        return false;
    }

    protected boolean take(@NotNull Token.Type... types) {
        for (Token.Type type : types) {
            if (match(type)) {
                consume();
                return true;
            }
        }
        return false;
    }

    protected Token consume() {
        if (index >= tokens.size()) {
            return Token.EOF;
        }
        var token = tokens.get(index);
        index += 1;
        return token;
    }

    protected List<Token> consumeTo(@NotNull Token.Type... types) {
        var tokens = new ArrayList<Token>();
        while (hasMoreTokens()) {
            var token = peekToken();
            if (Arrays.stream(types).anyMatch(t -> t.equals(token.type()))) {
                return tokens;
            }
            tokens.add(token);
            index += 1;
        }
        return null;
    }

    protected Token lastToken() {
        if (index == 0) return Token.SOF;
        return tokens.get(index - 1);
    }

    protected Token peekToken() {
        if (index >= tokens.size()) return Token.EOF;
        return tokens.get(index);
    }
}
