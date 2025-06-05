package mtmc.lang.sea;


import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.lang.CompilationException;
import mtmc.lang.ParseException;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SeaCompilationTests {
    Executable compile(String src) {
        var tokens = Token.tokenize(src);
        var parser = new SeaParser(tokens);
        Unit program;
        try {
            program = parser.parseUnit();
        } catch (ParseException e) {
            StringBuilder sb = new StringBuilder();
            Util.reportError(src, sb, e);
            fail(sb.toString());
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

    MonTanaMiniComputer compileAndRunM(String src) {
        var exe = compile(src);
        var machine = new MonTanaMiniComputer();
        machine.load(exe.code(), exe.data(), exe.debugInfo());
        machine.run();
        return machine;
    }

    String compileAndRun(String src) {
        var exe = compile(src);
        var machine = new MonTanaMiniComputer();
        machine.load(exe.code(), exe.data(), exe.debugInfo());
        machine.run();
        assertEquals(MonTanaMiniComputer.ComputerStatus.FINISHED, machine.getStatus());
        return machine.getConsole().getOutput();
    }

    @Test
    public void testR0() {
        var pgm = """
                int main(char *arg) {
                    return 42;
                }
                """;

        var mach = compileAndRunM(pgm);
        assertEquals(42, mach.getRegisterValue(Register.RV));
    }

    @Test
    public void helloWorld() {
        var pgm = """
                void puts(char *s);
                char *s = "hello, world";
                
                int main(char *arg) {
                    puts(s);
                    return 0;
                }
                """;

        String output = compileAndRun(pgm);
        assertEquals("hello, world", output);
    }

    @Test
    public void charLiteralWorks() {
        var pgm = """
                void putc(char c);
                
                int main() {
                    putc('W');
                    return 0;
                }
                """;

        String output = compileAndRun(pgm);
        assertEquals("W", output);
    }

    @Test
    public void testLiteralInt() {
        var pgm = """
                void putn(int v);
                void putc(char c);
                
                int main() {
                    int x = 3;
                    putn(x);
                    putc('\\n');
                    int y = x * 23;
                    putn(y);
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("3\n69", output);
    }

    @Test
    public void ternaryExpression() {
        var pgm = """
                void puts(char *s);
                
                int main() {
                    int x = 12;
                    char *a = x >= 12 ? "BIG\\n" : "little\\n";
                    char *b = x > 11 ? "BIG\\n" : "little\\n";
                    char *c = x < 13 ? "little\\n" : "BIG\\n";
                    char *d = x <= 12 ? "little\\n" : "BIG\\n";
                    puts(a);
                    puts(b);
                    puts(c);
                    puts(d);
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("BIG\nBIG\nlittle\nlittle\n", output);
    }
}
