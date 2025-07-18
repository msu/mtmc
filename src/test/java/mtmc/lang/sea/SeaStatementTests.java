package mtmc.lang.sea;

import mtmc.lang.ParseException;
import mtmc.lang.sea.ast.*;
import mtmc.lang.sea.ast.Error;
import org.junit.jupiter.api.Test;


import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

import static mtmc.lang.sea.SeaExpressionTests.Matching.*;
import static org.junit.jupiter.api.Assertions.*;

public class SeaStatementTests {
    List<Statement> parseStatements(String lex) {
        var tokens = Token.tokenize(lex);
        var parser = new SeaParser(null, lex, tokens);

        try {
            var stmts = new ArrayList<Statement>();
            while (parser.hasMoreTokens()) {
                var stmt = parser.parseStatement();
                if (stmt == null) fail("parsed null!");
                stmts.add(stmt);
            }
            if (parser.hasMoreTokens()) fail("more tokens in parser: " + parser.remainingTokens());

            var errors = new ArrayList<Error>();
            for (var stmt : stmts) {
                stmt.collectErrors(errors);
            }
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Error error : errors) {
                    Util.reportError(lex, sb, error.exception());
                }
                throw new ValidationException(errors, sb.toString());
            }

            return stmts;
        } catch (ParseException e) {
            var mb = new StringBuilder();
            Util.reportError(lex, mb, e);
            fail(mb.toString());
            return null;
        }
    }

    <T extends Statement> T parseStatement(String lex) {
        var tokens = Token.tokenize(lex);
        var parser = new SeaParser(null, lex, tokens);

        try {
            var stmt = parser.parseStatement();
            if (stmt == null) fail("parsed null!");
            if (parser.hasMoreTokens()) fail("more tokens in parser: " + parser.remainingTokens());

            var errors = stmt.collectErrors();
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Error error : errors) {
                    Util.reportError(lex, sb, error.exception());
                }
                throw new ValidationException(errors, sb.toString());
            }

            return (T) stmt;
        } catch (ParseException e) {
            var mb = new StringBuilder();
            Util.reportError(lex, mb, e);
            fail(mb.toString());
            return null;
        }
    }

    @Test
    public void testSimpleVariable() {
        StatementVar stmt = parseStatement("int myVar = 12;");
        assertEquals("myVar", stmt.name.content());
        assertInstanceOf(TypeExprInt.class, stmt.type);

        var val = ((ExpressionInteger) stmt.initValue);
        assertEquals(12, val.value);
    }

    @Test
    public void testEmptyVariable() {
        StatementVar stmt = parseStatement("int myVar;");
        assertEquals("myVar", stmt.name.content());
        assertInstanceOf(TypeExprInt.class, stmt.type);
        assertNull(stmt.initValue);
    }

    @Test
    public void testIfStatement() {
        StatementIf stmt = parseStatement("if (x == 3) printf(\"hello\");");
        assertMatches(stmt.condition, eq(sym("x"), intV(3)));
        assertMatches(stmt.body, call("printf", strV("hello")));
        assertNull(stmt.elseBody);
    }

    @Test
    public void testForStatement() {
        StatementFor stmt = parseStatement("for (; i < 10; i++) printf(\"hi there %s\", \"yo\");");
        assertNull(stmt.initStatement);
        assertNull(stmt.initExpression);
        assertMatches(stmt.condition, lt(sym("i"), intV(10)));
        assertMatches(stmt.inc, postInc(sym("i")));
        assertMatches(stmt.body, call("printf", strV("hi there %s"), strV("yo")));
    }

    @Test
    public void testIfElseStatement() {
        StatementIf stmt = parseStatement("""
                if (x >> 3 | y << 7) {
                    printf("x = %d, y = %d\\n", x, y);
                    x = x * 3 + y;
                } else {
                    printf("you\\n");
                }
                """);

        assertMatches(stmt.condition, bor(rsh(sym("x"), intV(3)), lsh(sym("y"), intV(7))));
        assertMatches(stmt.body, block(
                call("printf", strV("x = %d, y = %d\n"), sym("x"), sym("y")),
                assign("x", add(mul(sym("x"), intV(3)), sym("y")))
        ));
        assertMatches(stmt.elseBody, block(
                call("printf", strV("you\n"))
        ));
    }

    @Test
    public void incorrectTypeAssignmentFails() {
        var except = assertThrows(ValidationException.class, () -> {
            parseStatement("""
                int x = "hello, world";
                """);
        });

        assertEquals(1, except.errors.size());
        assertInstanceOf(ExpressionTypeError.class, except.errors.getFirst());
    }

    @Test
    public void scopedForStatements() {
        var except = assertThrows(ValidationException.class, () -> {
            parseStatements("""
                    for (int x = 0; x < 2034; x++) {
                        x = x * 2;
                    }
                    int y = x;
                    """);
        });

        assertEquals(1, except.errors.size());
        assertInstanceOf(ExpressionTypeError.class, except.errors.getFirst());
    }
}
