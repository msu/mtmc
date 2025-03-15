package edu.montana.cs.mtmc.os.shell;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

public abstract class ShellCommand {
    abstract void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception;
    public abstract String getHelp();
    public void usageException() {
        throw new UsageException(this);
    }
}
