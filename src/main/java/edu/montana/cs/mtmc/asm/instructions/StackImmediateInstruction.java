package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class StackImmediateInstruction extends Instruction {

    public StackImmediateInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public static final int MAX = 2^8 - 1;

    private MTMCToken stackRegisterToken;
    private MTMCToken valueToken;

    @Override
    public void genCode(short[] output) {
        int stackReg = Registers.SP;
        if(stackRegisterToken != null) {
            stackReg = Registers.toInteger(stackRegisterToken.getStringValue());
        }
        int value = valueToken.getIntegerValue();
        output[getLocation()] = (byte) (0b0010_0011_0000_0000 | value << 8 | stackReg);
    }

    public void setStackRegister(MTMCToken stackRegister) {
        this.stackRegisterToken = stackRegister;
    }

    public void setValue(MTMCToken valueToken) {
        this.valueToken = valueToken;
    }
}
