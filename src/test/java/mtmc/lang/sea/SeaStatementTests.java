package mtmc.lang.sea;

import mtmc.lang.ParseException;
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
            var mb = new StringBuilder();
            mb.append("Error:\n");
            for (var msg : e.messages) {
                var lo = Token.getLineAndOffset(lex, msg.start().start());
                int lineNo = lo[0];
                int column = lo[1];
                var line = Token.getLineFor(lex, msg.start().start());
                String prefix = "  %03d:%03d | ".formatted(lineNo, column);
                String info = " ".repeat(prefix.length() - 2) + "| ";
                mb.append(info).append(msg.message()).append('\n');
                mb.append(prefix).append(line).append('\n');
                mb
                        .repeat(' ', column - 1)
                        .repeat('^', Math.max(1, msg.end().end() - msg.start().start()));
                mb.append("\n\n");
            }
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

    }
}
