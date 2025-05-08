package mtmc.lang.sea;

import mtmc.lang.sea.ast.Expression;
import mtmc.lang.sea.ast.ExpressionInteger;
import mtmc.lang.sea.ast.ExpressionString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SeaExpressionTests {
    <T extends Expression> T parseExpr(String lex) {
        var tokens = Token.tokenize(lex);
        var parser = new SeaParser(tokens);
        var expr = parser.parseExpression();
        if (expr == null) fail("parsed null!");
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
}
