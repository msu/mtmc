package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssembleCommand extends ShellCommand {
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

        String dst = tokens.collapseTokensAsString();
        if (dst == null || dst.isBlank()) {
            throw new IllegalArgumentException("missing required argument 'dst'");
        }
        Path dstPath = getDiskPath(dst);

        String contents = Files.readString(srcPath);
        Assembler assembler = new Assembler();
        var file_name = srcPath.toString().substring(DISK_PATH.toString().length()).replaceAll("\\\\", "/");
        var executable = assembler.assembleExecutable(file_name, contents);
        executable.dump(dstPath);
    }

    @Override
    public String getHelp() {
        return """
                assemble <src> <dst>
                    - src : path to a .asm file
                    - dst : path to a target output binary""";
    }
}
