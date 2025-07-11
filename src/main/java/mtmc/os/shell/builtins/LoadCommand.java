package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.exec.Executable;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCToken;
import mtmc.tokenizer.MTMCTokenizer;

import java.nio.file.Path;
import mtmc.os.fs.FileSystem;

public class LoadCommand extends ShellCommand {
    static Path getDiskPath(String pathString, FileSystem fs) {
        Path path = Path.of("disk" + fs.resolve(pathString));
        return path.toAbsolutePath();
    }

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        FileSystem fs = computer.getFileSystem();
        String program = tokens.collapseTokensAsString();
        if (program == null || program.isBlank()) {
            throw new IllegalArgumentException("missing or required argument 'src'");
        }
        Path srcPath = getDiskPath(program, fs);

        Executable exec = Executable.load(srcPath);
        computer.load(exec.code(), exec.data(), exec.graphics(), exec.debugInfo());
        String source = tokens.getSource();

        // set up an argument if given
        if (tokens.more()) {
            MTMCToken firstArgToken = tokens.consume();
            int startChar = firstArgToken.start();
            String arg = source.substring(startChar);
            String strippedArg = arg.strip();
            if(!strippedArg.isEmpty()) {
                computer.setArg(strippedArg);
            }
        }
    }

    @Override
    public String getHelp() {
        return """
                load <exec>
                    - exec : path to an executable file""";
    }
}
