package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public class RunCommand extends ShellCommand {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) {
        switch (computer.getStatus()) {
            case READY, EXECUTING -> computer.run();
            default -> {
            }
        }
    }

    @Override
    public String getHelp() {
        return "run - runs the program until it halts";
    }
}
