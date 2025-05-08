package mtmc.lang.sea;

import mtmc.lang.sea.ast.ExpressionInteger;
import mtmc.lang.sea.ast.Statement;
import mtmc.lang.sea.ast.StatementVar;
import mtmc.lang.sea.ast.TypeExprInt;
import org.junit.jupiter.api.Test;

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
}
