package mtmc.lang.sea;

import mtmc.lang.sea.ast.*;
import org.junit.jupiter.api.Test;

import static mtmc.lang.sea.SeaExpressionTests.Matching.*;
import static org.junit.jupiter.api.Assertions.*;

public class SeaStatementTests {
    <T extends Statement> T parseStatement(String lex) {
        var tokens = Token.tokenize(lex);
        var parser = new SeaParser(tokens);

        try {
            var stmt = parser.parseStatement();
            if (stmt == null) fail("parsed null!");
            if (parser.hasMoreTokens()) fail("more tokens in parser: " + parser.remainingTokens());
            return (T) stmt;
        } catch (ParseException e) {
            var lo = Token.getLineAndOffset(lex, e.token.start());
            int lineNo = lo[0];
            int column = lo[1];
            var line = Token.getLineFor(lex, e.token.start());
            var msg = "error at " + lineNo + ":" + column + ":\n" + e.getMessage() + "\n\n | " + line
                    + "\n : " + (" ".repeat(column - 1) + "^".repeat(Math.max(1, e.token.end() - e.token.start())));
            fail(msg);
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

}
