package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCToken;
import mtmc.tokenizer.MTMCTokenizer;

import static mtmc.tokenizer.MTMCToken.TokenType.*;

public class SetCommand extends ShellCommand {

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        MTMCToken memLocation = tokens.matchAndConsume(INTEGER, HEX, BINARY);
        if (memLocation == null) {
            MTMCToken register = tokens.matchAndConsume(IDENTIFIER);
            if (register == null) usageException();
            MTMCToken value = tokens.matchAndConsume(INTEGER, HEX, BINARY);
            if (value == null) usageException();
            int reg = Register.toInteger(register.stringValue());
            if (reg >= 0) {
                computer.setRegisterValue(reg, value.intValue());
            } else {
                throw new IllegalArgumentException("Bad register: " + register.stringValue());
            }
        } else {
            MTMCToken value = tokens.matchAndConsume(INTEGER, HEX, BINARY, STRING);
            if (value == null) usageException();
            if (value.type() == INTEGER || value.type() == BINARY || value.type() == HEX) {
                computer.writeWordToMemory(memLocation.intValue(), value.intValue().shortValue());
            } else {
                computer.writeStringToMemory(memLocation.intValue(), value.stringValue().getBytes());
            }
        }
    }

    @Override
    public String getHelp() {
        return """
                set <loc> <value>- sets a memory location value
                     loc:   a register name or memory location
                     value: an integer, hex or binary value, or, for memory locations, a quoted string""";
    }
}
