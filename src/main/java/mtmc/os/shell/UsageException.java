package mtmc.os.shell;

public class UsageException extends RuntimeException {
    private final ShellCommand cmd;
    public UsageException(ShellCommand shellCommand) {
        super("Usage:\n\n" + shellCommand.getHelp());
        this.cmd = shellCommand;
    }
    public ShellCommand getCmd() {
        return cmd;
    }
}
