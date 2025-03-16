package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.Shell;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public class HelpCommand extends ShellCommand {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        Shell.printShellHelp(computer);
    }

    @Override
    public String getHelp() {
        return "? or help - print help";
    }
}
