package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class JumpInstruction extends Instruction {

    public static final int MAX = (1 << 12) - 1;
    private MTMCToken addressToken;

    public JumpInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    @Override
    public void genCode(short[] output) {
        int opcode = 0;
        switch (getType()) {
            case J -> opcode = 0b1100;
            case JZ -> opcode = 0b1101;
            case JNZ -> opcode = 0b1110;
            case JAL -> opcode = 0b1111;
        }
        int address = addressToken.getIntegerValue();
        output[getLocation()] = (short) (opcode << 12 | address);
    }

    public void setAddressToken(MTMCToken addressToken) {
        this.addressToken = addressToken;
    }
}
