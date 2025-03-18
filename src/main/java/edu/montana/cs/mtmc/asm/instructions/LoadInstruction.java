package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class LoadInstruction extends Instruction {

    public LoadInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    private MTMCToken targetToken;
    private MTMCToken pointerToken;
    private MTMCToken offsetToken;

    @Override
    public void genCode(short[] output) {
        int opcode = 0;
        switch (getType()) {
            case LW -> opcode = 0b00;
            case LB -> opcode = 0b01;
            case SW -> opcode = 0b10;
            case SB -> opcode = 0b11;
        }
        int target = Registers.toInteger(targetToken.getStringValue());
        int pointer = Registers.toInteger(pointerToken.getStringValue());
        int offset = Registers.ZERO;
        if (offsetToken != null) {
            offset = Registers.toInteger(offsetToken.getStringValue());
        }
        output[getLocation()] = (short) (0b0100_0000_0000_0000 | opcode << 12 | target << 8 | pointer << 4 | offset);
    }

    public void setTargetToken(MTMCToken targetToken) {
        this.targetToken = targetToken;
    }

    public void setPointerToken(MTMCToken pointerToken) {
        this.pointerToken = pointerToken;
    }

    public void setOffsetToken(MTMCToken offsetToken) {
        this.offsetToken = offsetToken;
    }
}
