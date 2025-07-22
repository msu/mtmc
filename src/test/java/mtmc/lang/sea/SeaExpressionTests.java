package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;
import mtmc.util.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class SeaExpressionTests {
    static class Matching {
        public interface Expr {
        }

        public interface Stmt {
        }

        public record Lt(Expr lhs, Expr rhs) implements Expr {
        }

        public static Lt lt(Expr lhs, Expr rhs) {
            return new Lt(lhs, rhs);
        }
        public record Eq(Expr lhs, Expr rhs) implements Expr {
        }

        public static Eq eq(Expr lhs, Expr rhs) {
            return new Eq(lhs, rhs);
        }

        public record Sym(String name) implements Expr {
        }

        public record Bor(Expr lhs, Expr rhs) implements Expr {}
        public static Bor bor(Expr lhs, Expr rhs) {
            return new Bor(lhs, rhs);
        }

        public record Rsh(Expr lhs, Expr rhs) implements Expr { }
        public static Rsh rsh(Expr lhs, Expr rhs) {
            return new Rsh(lhs, rhs);
        }

        public record Lsh(Expr lhs, Expr rhs) implements Expr { }
        public static Lsh lsh(Expr lhs, Expr rhs) {
            return new Lsh(lhs, rhs);
        }

        public record Assign(Expr lhs, Expr rhs) implements Expr { }
        public static Assign assign(String sym, Expr rhs) {
            return new Assign(new Sym(sym), rhs);
        }
        public static Assign assign(Expr lhs, Expr rhs) {
            return new Assign(lhs, rhs);
        }

        public record Add(Expr lhs, Expr rhs) implements Expr { }
        public static Add add(Expr lhs, Expr rhs) {
            return new Add(lhs, rhs);
        }

        public record Mul(Expr lhs, Expr rhs) implements Expr { }
        public static Mul mul(Expr lhs, Expr rhs) {
            return new Mul(lhs, rhs);
        }

        public static Sym sym(String name) {
            return new Sym(name);
        }

        public record Int(int value) implements Expr {
        }

        public static Int intV(int value) {
            return new Int(value);
        }

        public record Str(String value) implements Expr {
            @Override
            public String toString() {
                return "Str{value=" + StringEscapeUtils.escapeString(value) + '}';
            }
        }

        public static Str strV(String value) {
            return new Str(value);
        }

        public record Group(Expr inner) implements Expr {
        }

        public static Group group(Expr inner) {
            return new Group(inner);
        }

        public record Call(Expr method, Expr... args) implements Expr {
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Call call)) return false;
                return Objects.equals(method, call.method) && Objects.deepEquals(args, call.args);
            }

            @Override
            public int hashCode() {
                return Objects.hash(method, Arrays.hashCode(args));
            }

            @Override
            public String toString() {
                return "Call[" +
                        "method=" + method +
                        ", args=" + Arrays.toString(args) +
                        ']';
            }
        }

        public static Call call(String method, Expr... args) {
            return new Call(sym(method), args);
        }

        public static Call call(Expr method, Expr... args) {
            return new Call(method, args);
        }

        public record PostInc(Expr inner) implements Expr { }
        public static PostInc postInc(Expr inner) {
            return new PostInc(inner);
        }

        public record StmtExpr(Expr expr) implements Stmt {
        }

        public static StmtExpr expr(Expr expr) {
            return new StmtExpr(expr);
        }

        public record Block(Stmt... args) implements Stmt {
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Block block)) return false;
                return Objects.deepEquals(args, block.args);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(args);
            }

            @Override
            public String toString() {
                return "Block[" +
                        "args=" + Arrays.toString(args) +
                        ']';
            }
        }

        public static Block block(Object... items) {
            Stmt[] stmts = new Stmt[items.length];
            for (int i = 0; i < items.length; i++) {
                Object item = items[i];
                if (item instanceof Stmt stmt) stmts[i] = stmt;
                else if (item instanceof Expr expr) stmts[i] = expr(expr);
                else throw new IllegalArgumentException("item must be an expression or statement");
            }
            return new Block(stmts);
        }


        private static Stmt e2e(Statement s) {
            return switch (s) {
                case StatementBlock b -> {
                    Stmt[] stmts = new Stmt[b.statements.size()];
                    List<Statement> statements = b.statements;
                    for (int i = 0; i < statements.size(); i++) {
                        stmts[i] = e2e(statements.get(i));
                    }
                    yield new Block(stmts);
                }
                case StatementExpression e -> new StmtExpr(e2e(e.expression));
                default -> throw new UnsupportedOperationException("no support for " + s.getClass().getSimpleName());
            };
        }

        private static Expr e2e(Expression e) {
            return switch (e) {
                case ExpressionInteger i -> new Int(i.value);
                case ExpressionString s -> new Str(s.content());
                case ExpressionParens s -> new Group(e2e(s.inner));
                case ExpressionIdent i -> new Sym(i.name());
                case ExpressionBin bin -> switch (bin.op.content()) {
                    case "==" -> new Eq(e2e(bin.lhs), e2e(bin.rhs));
                    case "<" -> new Lt(e2e(bin.lhs), e2e(bin.rhs));
                    case "|" -> new Bor(e2e(bin.lhs), e2e(bin.rhs));
                    case ">>" -> new Rsh(e2e(bin.lhs), e2e(bin.rhs));
                    case "<<" -> new Lsh(e2e(bin.lhs), e2e(bin.rhs));
                    case "*" -> new Mul(e2e(bin.lhs), e2e(bin.rhs));
                    case "+" -> new Add(e2e(bin.lhs), e2e(bin.rhs));
                    case "=" -> new Assign(e2e(bin.lhs), e2e(bin.rhs));
                    case String op -> throw new UnsupportedOperationException("haven't done '" + op + "' yet");
                };
                case ExpressionPostfix pf -> switch (pf.op()) {
                    case "++" -> new PostInc(e2e(pf.inner));
                    case String op -> throw new UnsupportedOperationException("haven't done '" + op + "' yet");
                };
                case ExpressionCall call -> {
                    Expr[] args = new Expr[call.args.size()];
                    for (int i = 0; i < call.args.size(); i++) {
                        args[i] = e2e(call.args.get(i));
                    }
                    yield call(e2e(call.functor), args);
                }
                case Expression expr ->
                        throw new UnsupportedOperationException("unimplemented, e2e for " + expr.getClass().getSimpleName());
            };
        }

        public static void assertMatches(Expression expression, Expr e) {
            var e2 = e2e(expression);
            assertEquals(e2, e);
        }

        public static void assertMatches(Statement stmt, Expr e) {
            StmtExpr e1 = expr(e);
            var e2 = e2e(stmt);
            assertEquals(e1, e2);
        }

        public static void assertMatches(Statement expression, Stmt e) {
            var e2 = e2e(expression);
            assertEquals(e2, e);
        }
    }

    <T extends Expression> T parseExpr(String lex) {
        var tokens = Token.tokenize(lex);
        var parser = new SeaParser(null, lex, tokens);
        var expr = parser.parseExpression();
        if (expr == null) fail("parsed null!");
        var errors = expr.collectErrors();
        if (!errors.isEmpty()) {
            var mb = new StringBuilder();
            for (var error : errors) {
                mb.append("An error occurred\n");
                for (var msg : error.exception().messages) {
                    var lo = Token.getLineAndOffset(lex, msg.start().start());
                    int lineNo = lo[0];
                    int column = lo[1];
                    var line = Token.getLineFor(lex, msg.start().start());
                    String prefix = "  %03d:%03d | ".formatted(lineNo, column);
                    String info = " ".repeat(prefix.length());
                    mb.append(info).append("error: ").append(msg.message()).append('\n');
                    mb.append(prefix).append(line).append('\n');
                    mb
                            .repeat(' ', prefix.length() + column - 1)
                            .repeat('^', Math.max(1, msg.end().end() - msg.start().start()));
                    mb.append("\n\n");
                }
            }
            throw new ValidationException(errors, mb.toString());
        }
        if (parser.hasMoreTokens()) fail("more tokens in parser: " + parser.remainingTokens());
        return (T) expr;
    }

    @Test
    public void testLiteralInt() {
        ExpressionInteger expr = parseExpr("1");
        assertEquals(1, expr.value);
    }

    @Test
    public void testLiteralString() {
        ExpressionString expr = parseExpr("\"Hello, world\\n\"");
        assertArrayEquals("Hello, world\n".getBytes(), expr.getBytes());
    }

    @Test
    public void testStringMultiplicationFails() {
        var except = assertThrows(ValidationException.class, () -> {
            parseExpr("""
                "hello, world" * 8
                """);
        });

        assertEquals(1, except.errors.size());
        assertInstanceOf(ExpressionTypeError.class, except.errors.getFirst());
    }

    @Test
    public void invalidCasts() {
        // TODO: there are non yet, wait for structs
    }
}
