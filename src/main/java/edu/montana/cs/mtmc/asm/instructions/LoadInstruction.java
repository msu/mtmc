package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.Assembler;
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
    public void genCode(byte[] output, Assembler assembler) {
        int opcode = 0;
        switch (getType()) {
            case LW -> opcode = 0b0100;
            case LB -> opcode = 0b0101;
            case SW -> opcode = 0b0110;
            case SB -> opcode = 0b0111;
        }
        int target = Registers.toInteger(targetToken.stringValue());
        int pointer = Registers.toInteger(pointerToken.stringValue());
        int offset = Registers.ZERO;
        if (offsetToken != null) {
            offset = Registers.toInteger(offsetToken.stringValue());
        }
        output[getLocation()] = (byte) (opcode << 4 | target);
        output[getLocation() + 1] = (byte) (pointer << 4 | offset);
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
