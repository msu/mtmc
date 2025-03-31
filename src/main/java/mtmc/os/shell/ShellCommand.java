package mtmc.os.shell;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.tokenizer.MTMCTokenizer;

public abstract class ShellCommand {
    public abstract void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception;
    public abstract String getHelp();
    public void usageException() {
        throw new UsageException(this);
    }
}
