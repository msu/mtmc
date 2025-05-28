package mtmc.lang.sea;

import mtmc.lang.Language;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;

public class SeaLanguage implements Language {
    @Override
    public Executable compileExecutable(String source) {
        var tokens = Token.tokenize(source);
        var parser = new SeaParser(tokens);
        Unit program;
        try {
            program = parser.parseUnit();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        var compiler = new SeaCompiler(program);
        return compiler.compile();
    }
}
