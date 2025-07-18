package mtmc.lang;

import mtmc.os.exec.Executable;

public interface Language {
    Executable compileExecutable(String filename, String source) throws ParseException, CompilationException;
}
