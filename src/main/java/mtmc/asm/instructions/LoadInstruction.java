package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Registers;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

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

    public static String disassemble(short instruction) {
        if (getBits(16, 2, instruction) == 0b11) {
            StringBuilder builder = new StringBuilder("ldi ");
            short type = getBits(14, 2, instruction);
            if (type == 0b00) {
                builder.append("lw ");
            } else if (type == 0b01) {
                builder.append("lb ");
            } else if (type == 0b10) {
                builder.append("sw ");
            } else if (type == 0b11) {
                builder.append("sb ");
            }
            short srcDestReg = getBits(12, 4, instruction);
            short addrReg = getBits(8, 4, instruction);
            short offsetReg = getBits(4, 4, instruction);
            builder.append(Registers.fromInteger(srcDestReg)).append(" ");
            builder.append(Registers.fromInteger(addrReg)).append(" ");
            builder.append(Registers.fromInteger(offsetReg));
            return builder.toString();
        }
        return null;
    }

}
