package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.*;

public class SetCommand extends ShellCommand {

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        MTMCToken memLocation = tokens.matchAndConsume(INTEGER, HEX, BINARY);
        if (memLocation == null) {
            MTMCToken register = tokens.matchAndConsume(IDENTIFIER);
            if (register == null) usageException();
            MTMCToken value = tokens.matchAndConsume(INTEGER, HEX, BINARY);
            if (value == null) usageException();
            int reg = Registers.toInteger(register.stringValue());
            if (reg >= 0) {
                computer.setRegister(reg, value.intValue());
            } else {
                throw new IllegalArgumentException("Bad register: " + register.stringValue());
            }
        } else {
            MTMCToken value = tokens.matchAndConsume(INTEGER, HEX, BINARY, STRING);
            if (value == null) usageException();
            if (value.type() == INTEGER || value.type() == BINARY || value.type() == HEX) {
                computer.writeWord(memLocation.intValue(), value.intValue().shortValue());
            } else {
                byte[] bytes = value.stringValue().getBytes();
                Integer start = memLocation.intValue();
                for (int i = 0; i < bytes.length; i++) {
                    byte aByte = bytes[i];
                    computer.writeByte(start + i, aByte);
                }
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
