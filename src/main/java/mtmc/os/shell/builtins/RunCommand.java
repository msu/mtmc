package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

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
