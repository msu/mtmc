package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

public class ExitCommand extends ShellCommand {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) {
        computer.getConsole().println("Goodbye!");
        System.exit(1);
    }

    @Override
    public String getHelp() {
        return "exit - exits the system";
    }
}
