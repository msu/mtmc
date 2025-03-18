package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class LoadImmediateInstruction extends Instruction {

    public LoadImmediateInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public static final int MAX = 2^12 - 1;

    private MTMCToken tempRegisterToken;
    private MTMCToken valueToken;

    @Override
    public void genCode(short[] output) {
        int reg = Registers.toInteger(tempRegisterToken.getStringValue());
        int value = valueToken.getIntegerValue();
        output[getLocation()] = (byte) (0b1000_0011_0000_0000 | reg << 12 | value << 8);
    }

    public void setTempRegister(MTMCToken stackRegister) {
        this.tempRegisterToken = stackRegister;
    }

    public void setValue(MTMCToken valueToken) {
        this.valueToken = valueToken;
    }
}
