package mtmc.lang.sea;

import mtmc.lang.CompilationException;
import mtmc.lang.Language;
import mtmc.lang.sea.ast.Unit;
import mtmc.os.exec.Executable;

public class SeaLanguage implements Language {
    @Override
    public Executable compileExecutable(String source) throws mtmc.lang.ParseException, CompilationException  {
        var tokens = Token.tokenize(source);
        var parser = new SeaParser(tokens);
        Unit program = parser.parseUnit();

        var compiler = new SeaCompiler(program);
        return compiler.compile();
    }
}
