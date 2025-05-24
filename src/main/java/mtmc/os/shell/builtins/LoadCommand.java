package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.exec.Executable;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LoadCommand extends ShellCommand {
    static final Path DISK_PATH = Path.of(System.getProperty("user.dir"), "disk").toAbsolutePath();

    static Path getDiskPath(String pathString) {
        Path path = Paths.get(pathString);
        if (!path.isAbsolute()) {
            path = DISK_PATH.resolve(path);
        }
        path = path.toAbsolutePath();
        if (!path.startsWith(DISK_PATH)) {
            throw new IllegalArgumentException(pathString + " is not a disk path");
        }
        return path;
    }

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        String src = tokens.collapseTokensAsString();
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("missing or required argument 'src'");
        }
        Path srcPath = getDiskPath(src);

        Executable exec = Executable.load(srcPath);
        computer.load(exec.code(), exec.data(), exec.debugInfo());
    }

    @Override
    public String getHelp() {
        return """
                load <exec>
                    - exec : path to an executable file""";
    }
}
