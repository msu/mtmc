package mtmc.lang;

import mtmc.os.exec.Executable;

public interface Language {
    Executable compileExecutable(String source) throws ParseException, CompilationException;
}
