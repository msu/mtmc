package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public class PauseCommand extends ShellCommand {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) {
        computer.pause();
    }

    @Override
    public String getHelp() {
        return "pause - pauses the computer";
    }
}

