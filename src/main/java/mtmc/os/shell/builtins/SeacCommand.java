package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.lang.sea.SeaLanguage;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;
import mtmc.util.StringEscapeUtils;

public class SeacCommand extends ShellCommand  {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        String output = "a.out";
        String filename = null;
        while (tokens.more()) {
            var token = tokens.collapseTokensAsString();
            if (token.equals("-o")) {
                if (!tokens.more()) throw new IllegalArgumentException("expected filename after '-o'");
                output = tokens.collapseTokensAsString();
            } else {
                filename = tokens.collapseTokensAsString();
            }
        }

        if (filename == null) {
            throw new IllegalArgumentException("expected source file");
        }

        if (!computer.getFileSystem().exists(filename)) {
            throw new IllegalArgumentException("file " + StringEscapeUtils.escapeString(filename) + " does not exist");
        }

        SeaLanguage lang = new SeaLanguage();
        String content = computer.getFileSystem().readFile(filename);
        var exec = lang.compileExecutable(filename, content);

        String bin = exec.dump();
        computer.getFileSystem().writeFile(output, bin);
    }

    @Override
    public String getHelp() {
        return """
               """;
    }
}
