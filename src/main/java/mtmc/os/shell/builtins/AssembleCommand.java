package mtmc.os.shell.builtins;

import mtmc.asm.Assembler;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

import java.nio.file.Files;
import java.nio.file.Path;
import mtmc.os.fs.FileSystem;

public class AssembleCommand extends ShellCommand {
    static Path getDiskPath(String pathString, FileSystem fs) {
        Path path = Path.of("disk" + fs.resolve(pathString));
        return path.toAbsolutePath();
    }

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        FileSystem fs = computer.getFileSystem();
        String src = tokens.collapseTokensAsString();
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("missing or required argument 'src'");
        }
        Path srcPath = getDiskPath(src, fs);

        String dst = tokens.collapseTokensAsString();
        if (dst == null || dst.isBlank()) {
            throw new IllegalArgumentException("missing required argument 'dst'");
        }
        Path dstPath = getDiskPath(dst, fs);

        String contents = Files.readString(srcPath);
        Assembler assembler = new Assembler();
        var file_name = fs.resolve(src); // srcPath.toString().substring(DISK_PATH.toString().length()).replaceAll("\\\\", "/");
        var executable = assembler.assembleExecutable(file_name, contents);
        executable.dump(dstPath);
        computer.notifyOfFileSystemUpdate();
    }

    @Override
    public String getHelp() {
        return """
                asm <src> <dst>
                    - src : path to a .asm file
                    - dst : path to a target output binary""";
    }
}
