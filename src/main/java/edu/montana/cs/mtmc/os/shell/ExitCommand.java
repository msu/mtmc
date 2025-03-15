package edu.montana.cs.mtmc.os.shell;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public class ExitCommand extends ShellCommand {
    @Override
    void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) {
        computer.getConsole().println("Goodbye!");
        System.exit(1);
    }

    @Override
    public String getHelp() {
        return "exit - exits the system";
    }
}
