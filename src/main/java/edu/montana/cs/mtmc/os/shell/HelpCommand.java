package edu.montana.cs.mtmc.os.shell;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public class HelpCommand extends ShellCommand {
    @Override
    void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        Shell.printShellHelp(computer);
    }

    @Override
    public String getHelp() {
        return "? or help - print help";
    }
}
