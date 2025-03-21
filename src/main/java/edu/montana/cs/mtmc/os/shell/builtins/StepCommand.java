package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public class StepCommand extends ShellCommand {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) {
        switch (computer.getStatus()) {
            case READY -> {
                computer.setStatus(MonTanaMiniComputer.ComputerStatus.EXECUTING);
                computer.fetchAndExecute();
            }
            case EXECUTING -> computer.fetchAndExecute();
            default -> {
            }
        }
    }

    @Override
    public String getHelp() {
        return "step - runs the next instruction";
    }
}

