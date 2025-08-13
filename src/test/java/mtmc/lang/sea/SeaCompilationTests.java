package mtmc.lang.sea;


import mtmc.emulator.DebugInfo;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.lang.CompilationException;
import mtmc.lang.ParseException;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;
import mtmc.web.WebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SeaCompilationTests {
    Executable compile(String src) {
        var tokens = Token.tokenize(src);
        var parser = new SeaParser(null, src, tokens);
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
                    System.out.println("on line " + lo[0] + ", column " + lo[1]);
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

    String compileAndRun(boolean server, String src) {
        var exe = compile(src);
        exe = new Executable(exe.format(), exe.code(), exe.data(), exe.graphics(), "/src/test.sea", new DebugInfo(
                exe.debugInfo().debugStrings(),
                "/src/test.sea",
                exe.debugInfo().assemblySource(),
                exe.debugInfo().assemblyLineNumbers(),
                "/src/test.sea",
                exe.debugInfo().originalLineNumbers(),
                exe.debugInfo().globals(),
                exe.debugInfo().locals()
        ));
        var machine = new MonTanaMiniComputer();
        if (!server) {
            machine.load(exe.code(), exe.data(), exe.graphics(), exe.debugInfo());
            machine.run();
        } else {
            var ws = new WebServer(machine);
            var fs = ws.getComputerView().getFileSystem();

            try {
                fs.writeFile("/src/test.sea", src);
                fs.writeFile("/bin/test", exe.dump());
                machine.load(exe.code(), exe.data(), exe.graphics(), exe.debugInfo());
                ws.getComputerView().openFile("/src/test.sea");
                ws.getComputerView().applyBreakpoints();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        assertEquals(MonTanaMiniComputer.ComputerStatus.FINISHED, machine.getStatus());
        return machine.getConsole().getOutput();
    }

    String compileAndRun(String src) {
        var exe = compile(src);
        var machine = new MonTanaMiniComputer();
        machine.load(exe.code(), exe.data(), exe.debugInfo());
        machine.run();
//        var ws = new WebServer(machine);
//        try {
//            Thread.sleep(Long.MAX_VALUE);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
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
    public void printf() {
        var pgm = """
                int printf(char *s, ...);
                void putn(int v);
                
                int main() {
                    int x = 32;
                    char z = 'Z';
                    char *s = "hello, my name is pathfinder!";
                    int n = printf("%d %c %s\\n", x, z, s);
                    putn(n);
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("32 Z hello, my name is pathfinder!\n35", output);
    }

    @Test
    public void simpleArith() {
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
    public void simpleTernary() {
        var pgm = """
                void puts(char *s);
                
                int main() {
                    char *s = 0 ? "fail\\n" : "pass\\n";
                    puts(s);
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("pass\n", output);
    }

    @Test
    public void comparison() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int a = 4 < 53;
                    int b = 4 > 4;
                    int c = 912 <= 3;
                    printf("%d %d %d\\n", a, b, c);
                    return 0;
                }
                """;

        String output = compileAndRun(pgm);
        assertEquals("1 0 0\n", output);
    }

    @Test
    public void not() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    printf("%d, %d, %d\\n", !0, !1, !-23);
                    return 0;
                }
                """;

        String output = compileAndRun(pgm);
        assertEquals("1, 0, 0\n", output);
    }

    @Test
    public void conjunctive() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int x = 12;
                    int y = 3;
                    int a = x && y;
                    int b = x && !y;
                    int c = !x && y;
                    int d = !x && !y;
                    printf("TT: %d %d %d %d\\n", a, b, c, d);
                    return 0;
                }
                """;

        String output = compileAndRun(pgm);
        assertEquals("TT: 1 0 0 0\n", output);
    }

    @Test
    public void antiConjunctive() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int x = 12;
                    int y = 3;
                    int a = !(x && y);
                    int b = !(x && !y);
                    int c = !(!x && y);
                    int d = !(!x && !y);
                    printf("TT: %d %d %d %d\\n", a, b, c, d);
                    return 0;
                }
                """;

        String output = compileAndRun(pgm);
        assertEquals("TT: 0 1 1 1\n", output);
    }

    @Test
    public void disjunctive() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int x = 12;
                    int y = 3;
                    int a = x || y;
                    int b = x || !y;
                    int c = !x || y;
                    int d = !x || !y;
                    printf("TT: %d %d %d %d\\n", a, b, c, d);
                    return 0;
                }
                """;
        String output = compileAndRun(pgm);
        assertEquals("TT: 1 1 1 0\n", output);
    }

    @Test
    public void antiDisjunctive() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int x = 12;
                    int y = 3;
                    int a = !(x || y);
                    int b = !(x || !y);
                    int c = !(!x || y);
                    int d = !(!x || !y);
                    printf("TT: %d %d %d %d\\n", a, b, c, d);
                    return 0;
                }
                """;
        String output = compileAndRun(pgm);
        assertEquals("TT: 0 0 0 1\n", output);
    }

    @Test
    public void complexLogical() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int x = 3;
                    int y = 0;
                    int z = 8;
                    int q = 4;
                
                    int a = x || y && z;
                    int b = x && y && z;
                    int c = x && !y && z;
                    int d = !x || y || !z;
                    int e = x && z && q;
                
                    printf("%d %d %d %d %d\\n", a, b, c, d, e);
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("1 0 1 0 1\n", output);
    }

    @Test
    public void equality() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int a = 1 == 1;
                    int b = 2 == 1;
                    int c = 834 != 3822;
                    printf("%d %d %d\\n", a, b, c);
                    return 0;
                }
                """;
        String output = compileAndRun(pgm);
        assertEquals("1 0 1\n", output);
    }


    @Test
    public void ifStmt() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int x = 13;
                    int y = 42;
                
                    if (x > y || y / x == 3) {
                        printf("yo!\\n");
                    }
                
                    if (x > y) {
                        printf("never\\n");
                    } else {
                        printf("I'm a little teapot\\n");
                    }
                
                    return 0;
                }
                """;
        String output = compileAndRun(pgm);
        assertEquals("yo!\nI'm a little teapot\n", output);
    }

    @Test
    public void doWhileStmt() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int a = 1;
                    int b = 1;
                    printf("%d %d ", a, b);
                
                    do {
                        a = a + b;
                        b = a - b;
                        printf("%d ", a);
                    } while (a < 100 || b < 100);
                
                    return 0;
                }
                """;
        String output = compileAndRun(pgm);
        assertEquals("1 1 2 3 5 8 13 21 34 55 89 144 233 ", output);
    }

    @Test
    public void collatzWhile() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                
                    int n = 12;
                    while (n != 1) {
                        printf("%d ", n);
                        if (n % 2 == 0) n /= 2;
                        else n = 3 * n + 1;
                    }
                
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("12 6 3 10 5 16 8 4 2 ", output);
    }

    @Test
    public void forLoop() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int sum = 0;
                    for (int i = 0; i < 10; i += 1) {
                        printf("%d ", i);
                        sum += i;
                    }
                    printf("\\nsum = %d\\n", sum);
                
                    for (sum = 3; sum < 25; sum *= 2) {
                        printf("%d ", sum);
                        if (sum % 2 == 0) break;
                        else continue;
                    }
                
                    return 0;
                }
                
                """;

        var output = compileAndRun(pgm);
        assertEquals("0 1 2 3 4 5 6 7 8 9 \nsum = 45\n3 6 ", output);
    }

    @Test
    public void fibHops() {
        var pgm = """
                int printf(char *s, ...);
                
                int main() {
                    int a = 1;
                    int b = 1;
                    goto printer;
                
                    fib:
                    a += b;
                    b = a - b;
                
                    printer:
                    printf("%d ", a);
                
                    if (a < 10000) goto fib;
                
                    return 0;
                }
                """;

        var output = compileAndRun(pgm);
        assertEquals("1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 6765 10946 ", output);
    }

    @Test
    public void scanfMe() {
        var output = compileAndRun("""
                
                """);
    }

    @Test
    public void structures() {
        var output = compileAndRun("""
                int printf(char *s, ...);
                
                struct Point {
                    int x;
                    int y;
                };
                
                int main() {
                    Point pt = {100, 10};
                    printf("Hello from %d, %d\\n", pt.x, pt.y);
                
                    Point p2 = {50, 50};
                    printf("Hello to %d, %d\\n", p2.x, p2.y);
                
                    int dy = p2.y - pt.y;
                    int dx = p2.x - pt.x;
                    printf("slope between points is %d/%d\\n", dy, dx);
                
                    return 0;
                }
                
                """);
        assertEquals("Hello from 100, 10\nHello to 50, 50\nslope between points is 40/-50\n", output);
    }

    @Test
    public void assignInStruct() {
        var output = compileAndRun("""
                int printf(char *s, ...);
                
                struct Person {
                    char *name;
                    int age;
                };
                
                int main() {
                    Person dev = {"dillon", 21};
                    printf("name = %s, age = %d\\n", dev.name, dev.age);
                    // summer 2025
                    dev.age = 22;
                    printf("name = %s, age = %d\\n", dev.name, dev.age);
                    return 0;
                }
                """);
        assertEquals("name = dillon, age = 21\nname = dillon, age = 22\n", output);
    }

    @Test
    public void assignInEmbeddedStructs() {
        var output = compileAndRun("""
                int printf(char *s, ...);
                
                struct Address {
                    char *city;
                    char *state;
                };
                
                struct Person {
                    char *name;
                    int age;
                    Address addr;
                };
                
                int main() {
                    Person dev = {"dillon", 21, {"Bozeman", "MT"}};
                    printf("name = %s, age = %d\\n", dev.name, dev.age);
                    printf(" from %s, %s\\n", dev.addr.city, dev.addr.state);
                
                    // summer 2025
                    dev.age = 22;
                    dev.addr = {"Jamestown", "ND"};
                    printf("name = %s, age = %d\\n", dev.name, dev.age);
                    printf(" from %s, %s\\n", dev.addr.city, dev.addr.state);
                    return 0;
                }
                """);
        assertEquals("name = dillon, age = 21\n from Bozeman, MT\nname = dillon, age = 22\n from Jamestown, ND\n", output);
    }

    @Test
    public void testMultiplyStack() {
        var output = compileAndRun("""
                int printf(char *s, ...);
                
                int square(int i) {
                    return i * i;
                }
                
                int main() {
                    int x = square(3);
                    printf("%d\\n", x);
                    return 0;
                }
                """);

        assertEquals("9\n", output);
    }

    @Test
    public void argumenmtAssignment() {
        var output = compileAndRun("""
                int printf(char *s, ...);
                
                int double(int i) {
                    i = i + i;
                    return i;
                }
                
                int main() {
                    int x = double(3);
                    printf("%d\\n", x);
                    return 0;
                }
                
                """);

        assertEquals("6\n", output);
    }

    @Test
    public void globalArrayAssignment() {
        var output = compileAndRun(true, """
                int printf(char *s, ...);
                
                char s[4];
                
                int main() {
                    s[0] = 'h';
                    s[1] = 'e';
                    s[2] = 'y';
                    s[3] = 0;
                    printf("%s\\n", s);
                    return 0;
                }
                """);
    }
}
