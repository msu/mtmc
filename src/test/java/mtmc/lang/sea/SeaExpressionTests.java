package mtmc.lang.sea;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SeaExpressionTests {
    <T extends Ast.Expr> T parseExpr(String src) {
        List<Token> tokens;
        try {
            tokens = Token.tokenize(src);
        } catch (Token.TokenizeException e) {
            int[] info = Token.getLineAndOffset(src, e.start);

            StringBuilder msg = new StringBuilder();
            msg.append("at %d:%d".formatted(info[0], info[1]));
            msg.append(Token.highlight(src, e.start, e.end));
            msg.append("\n = error: ");
            msg.append(e.getMessage());
            fail(msg.toString());
            return null; // haha
        }

        var parser = new SeaParser(tokens);
        var expr = parser.parseExpr();
        if (expr == null) {
            fail("expected expression, none was found");
            return null;
        }

        var errors = collectErrors(expr).toList();
        StringBuilder msg = new StringBuilder();
        for (var error : errors) {
            int[] loc = Token.getLineAndOffset(src, error.start().start());
            msg.append("\nERROR: at %d:%d\n".formatted(loc[0], loc[1]));
            String highlight = Token.highlight(src, error.start().start(), error.end().end())
                    .lines()
                    .map(line -> " | " + line)
                    .collect(Collectors.joining("\n"));
            msg.append(highlight).append('\n');

            for (var pair : error.messages().entrySet()) {
                Token tok = pair.getKey();
                String message = pair.getValue();
                String hl = Token.highlight(src, tok.start(), tok.end())
                        .lines()
                        .map(line -> "   | " + line)
                        .collect(Collectors.joining("\n"));
                msg.append(" = info: ").append(message).append('\n');
                msg.append(hl).append("\n\n");
            }
        }

        if (!msg.isEmpty()) {
            fail(msg.toString());
        }

        return (T) expr;
    }

    static Stream<Ast.Error> collectErrors(Ast expr) {
        if (expr == null) return Stream.empty();

        var children = expr.getChildren()
                .flatMap(SeaExpressionTests::collectErrors);

        if (expr instanceof Ast.Error) {
            return Stream.concat(children, Stream.of((Ast.Error) expr));
        } else {
            return children;
        }
    }

    @Test
    public void testInteger() {
        Ast.Int expr = parseExpr("10");
        assertNotNull(expr);
        assertEquals(10, expr.value());
    }
}
