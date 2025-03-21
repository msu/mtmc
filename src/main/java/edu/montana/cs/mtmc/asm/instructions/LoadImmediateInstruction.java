package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class LoadImmediateInstruction extends Instruction {

    public LoadImmediateInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public static final int MAX = (1 << 12) - 1;

    private MTMCToken tempRegisterToken;
    private MTMCToken valueToken;

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int reg = Registers.toInteger(tempRegisterToken.stringValue());
        int value = resolveValue(assembler);
        output[getLocation()] = (byte) (0b1000_0000 | reg << 4 | value >>> 8);
        output[getLocation() + 1] = (byte) (value);
    }

    public void setTempRegister(MTMCToken stackRegister) {
        this.tempRegisterToken = stackRegister;
    }

    @Override
    public void validateLabel(Assembler assembler) {
        if (valueToken.type() == MTMCToken.TokenType.IDENTIFIER) {
            if (!assembler.hasLabel(valueToken.stringValue())) {
                addError("Unresolved label: " + valueToken.stringValue());
            }
        }
    }

    private Integer resolveValue(Assembler assembler) {
        if (valueToken.type() == MTMCToken.TokenType.IDENTIFIER) {
            return assembler.resolveLabel(valueToken.stringValue());
        } else {
            return valueToken.intValue();
        }
    }


    public void setValue(MTMCToken valueToken) {
        this.valueToken = valueToken;
    }
}
