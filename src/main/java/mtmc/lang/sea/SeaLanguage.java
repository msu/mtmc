package mtmc.lang.sea;

import mtmc.lang.CompilationException;
import mtmc.lang.Language;
import mtmc.lang.ParseException;
import mtmc.lang.sea.ast.Error;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;

public class SeaLanguage implements Language {
    @Override
    public Executable compileExecutable(String filename, String source) throws mtmc.lang.ParseException, CompilationException  {
        var tokens = Token.tokenize(source);
        var parser = new SeaParser(filename, source, tokens);
        Unit program = parser.parseUnit();
        var errors = program.collectErrors();
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Error error : errors) {
                reportError(source, sb, error.exception());
            }
            throw new RuntimeException(sb.toString());
        }


        var compiler = new SeaCompiler(program);
        return compiler.compile();
    }

    private static void reportError(String src, StringBuilder sb, ParseException e) {
        sb.append("Error:\n");
        for (var msg : e.messages) {
            var lo = Token.getLineAndOffset(src, msg.start().start());
            int lineNo = lo[0];
            int column = lo[1];
            var line = Token.getLineFor(src, msg.start().start());
            String prefix = "  %03d:%03d | ".formatted(lineNo, column);
            String info = " ".repeat(prefix.length() - 2) + "| ";
            sb.append(info).append(msg.message()).append('\n');
            sb.append(prefix).append(line).append('\n');
            sb
                    .repeat(' ', prefix.length() + column - 1)
                    .repeat('^', Math.max(1, msg.end().end() - msg.start().start()));
            sb.append("\n\n");
        }
    }
}
