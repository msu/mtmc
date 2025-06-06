package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class LoadStoreRegisterInstruction extends Instruction {

    private MTMCToken targetToken;
    private MTMCToken pointerToken;
    private MTMCToken offsetToken;

    public LoadStoreRegisterInstruction(InstructionType type, List<MTMCToken> label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
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

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opcode = 0;
        switch (getType()) {
            case LWR -> opcode = 0b0100;
            case LBR -> opcode = 0b0101;
            case SWR -> opcode = 0b0110;
            case SBR -> opcode = 0b0111;
        }
        int target = Register.toInteger(targetToken.stringValue());
        int pointer = Register.toInteger(pointerToken.stringValue());
        int offset = Register.PC.ordinal();
        if (offsetToken != null) {
            offset = Register.toInteger(offsetToken.stringValue());
        }
        output[getLocation()] = (byte) (opcode << 4 | target);
        output[getLocation() + 1] = (byte) (pointer << 4 | offset);
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 2, instruction) == 0b01) {
            StringBuilder builder = new StringBuilder();
            short type = getBits(14, 2, instruction);
            if (type == 0b00) {
                builder.append("lwr ");
            } else if (type == 0b01) {
                builder.append("lbr ");
            } else if (type == 0b10) {
                builder.append("swr ");
            } else if (type == 0b11) {
                builder.append("sbr ");
            }
            short srcDestReg = getBits(12, 4, instruction);
            short addrReg = getBits(8, 4, instruction);
            short offsetReg = getBits(4, 4, instruction);
            builder.append(Register.fromInteger(srcDestReg)).append(" ");
            builder.append(Register.fromInteger(addrReg)).append(" ");
            builder.append(Register.fromInteger(offsetReg));
            return builder.toString();
        }
        return null;
    }

}
