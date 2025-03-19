package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class JumpInstruction extends Instruction {

    public static final int MAX = (1 << 12) - 1;
    private MTMCToken addressToken;

    public JumpInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opcode = 0;
        switch (getType()) {
            case J -> opcode = 0b1100;
            case JZ -> opcode = 0b1101;
            case JNZ -> opcode = 0b1110;
            case JAL -> opcode = 0b1111;
        }
        int address = resolveTargetAddress(assembler);
        output[getLocation()] = (byte) (opcode << 4 | address >>> 8);
        output[getLocation()+1] = (byte) address;
    }

    @Override
    public void validateLabel(Assembler assembler) {
        if (addressToken.getType() == MTMCToken.TokenType.IDENTIFIER) {
            if (!assembler.hasLabel(addressToken.getStringValue())) {
                addError("Unresolved label: " + addressToken.getStringValue());
            }
        }
    }

    private Integer resolveTargetAddress(Assembler assembler) {
        if (addressToken.getType() == MTMCToken.TokenType.IDENTIFIER) {
            return assembler.resolveLabel(addressToken.getStringValue());
        } else {
            return addressToken.getIntegerValue();
        }
    }

    public void setAddressToken(MTMCToken addressToken) {
        this.addressToken = addressToken;
    }
}
