package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

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

