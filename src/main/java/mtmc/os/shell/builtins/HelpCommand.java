package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.Shell;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

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
