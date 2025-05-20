package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCToken;
import mtmc.tokenizer.MTMCTokenizer;

import static mtmc.tokenizer.MTMCToken.TokenType.*;

public class GetCommand extends ShellCommand {

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        MTMCToken memLocation = tokens.matchAndConsume(INTEGER, HEX, BINARY);
        if (memLocation == null) {
            MTMCToken register = tokens.matchAndConsume(IDENTIFIER);
            if (register == null) usageException();
            int reg = Register.toInteger(register.stringValue());
            if (reg >= 0) {
                computer.getConsole().println(register.stringValue() + ": " + computer.getRegisterValue(reg));;
            } else {
                throw new IllegalArgumentException("Bad register: " + register.stringValue());
            }
        } else {
            if (memLocation.type() == INTEGER || memLocation.type() == BINARY || memLocation.type() == HEX) {
                computer.getConsole().println("MEMORY " + memLocation.intValue() + ": " + computer.fetchWordFromMemory(memLocation.intValue()));;
            }
        }
    }

    @Override
    public String getHelp() {
        return """
                get <loc> - gets a memory location value
                     loc:   a register name or memory location
                """;
    }
}
