package mtmc.lang.sea;


import mtmc.emulator.MonTanaMiniComputer;
import mtmc.lang.CompilationException;
import mtmc.lang.ParseException;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;
import mtmc.web.WebServer;
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
                for (var msg : error.exception().messages) {
                    int[] lo = Token.getLineAndOffset(src, msg.span().start().start());
                    System.out.println("at " + lo[0] + ":" + lo[1]);
                    System.out.println("  " + msg.message());
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

    String compileAndRun(String src) {
        var exe = compile(src);
        var machine = new MonTanaMiniComputer();
        machine.load(exe.code(), exe.data(), exe.debugInfo());
        machine.run();
        return machine.getConsole().getOutput();
    }

    @Test
    public void testR0() {
        var pgm = """
                int main(char *arg) {
                    return 0;
                }
                """;

        var exec = compile(pgm);
    }

    @Test
    public void testSimpleVars() {
        var pgm = """
                void puts(char *s);
                char *s = "hello, world";
                
                int main(char *arg) {
                    puts(s);
                    return 0;
                }
                """;
    }

    @Test
    public void testLiteralInt() {
        var pgm = """
                void putn(int v);
                
                int main() {
                    int x = 3;
                    putn(x);
                    int y = 134;
                    putn(y);
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
    }
}
