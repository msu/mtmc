package mtmc.lang.sea;


import mtmc.lang.CompilationException;
import mtmc.lang.ParseException;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class SeaCompilationTests {
    Executable compile(String src) {
        var tokens = Token.tokenize(src);
        var parser = new SeaParser(tokens);
        Unit program;
        try {
            program = parser.parseUnit();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        var errors = program.collectErrors();
        if (!errors.isEmpty()) {
            for (var error : errors) {
                for (var msg : error.messages) {
                    int[] lo = Token.getLineAndOffset(src, msg.span().start().start());
                    System.out.println("at " + lo[0] + ":" + lo[1]);
                    System.out.println("  " + error.getMessage());
                }
            }
            fail();
        }

        var compiler = new SeaCompiler(program);
        try {
            return compiler.compile();
        } catch (CompilationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testR0() {
        var pgm = """
                int main() {
                    return 0;
                }
                """;

        var exec = compile(pgm);
    }
}
