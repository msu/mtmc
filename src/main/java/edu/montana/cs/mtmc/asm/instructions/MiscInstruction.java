package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.os.SysCall;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

import static edu.montana.cs.mtmc.util.BinaryUtils.getBits;

public class MiscInstruction extends Instruction {

    private MTMCToken syscallType;
    private MTMCToken fromRegister;
    private MTMCToken toRegister;

    public MiscInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        if (getType() == InstructionType.SYS) {
            output[getLocation()] = 0b0000_0000;
            output[getLocation() + 1] = SysCall.getValue(this.syscallType.stringValue());
        } else if (getType() == InstructionType.MV) {
            int to = Registers.toInteger(toRegister.stringValue());
            int from = Registers.toInteger(fromRegister.stringValue());
            output[getLocation()] = 0b0000_0001;
            output[getLocation() + 1] = (byte) (to << 4 | from);
        } else if(getType() == InstructionType.NOOP) {
            output[getLocation()] = (byte) (0b0000_1111);
            output[getLocation() + 1] = (byte) (0b1111_1111);
        }
    }


    public void setSyscallType(MTMCToken type) {
        this.syscallType = type;
    }

    public void setFrom(MTMCToken fromRegister) {
        this.fromRegister = fromRegister;
    }

    public void setTo(MTMCToken toRegister) {
        this.toRegister = toRegister;
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 0) {
            short type = getBits(12, 4, instruction);
            if (type == 0) {
                StringBuilder builder = new StringBuilder("sys ");
                short bits = getBits(8, 8, instruction);
                String name = SysCall.getString((byte) bits);
                builder.append(name);
                return builder.toString();
            } else if (type == 1) {
                StringBuilder builder = new StringBuilder("mv ");
                short to = getBits(8, 4, instruction);
                short from = getBits(4, 4, instruction);
                builder.append(Registers.fromInteger(to)).append(" ");
                builder.append(Registers.fromInteger(from)).append(" ");
                return builder.toString();
            } else if (type == 15) {
                return "noop";
            }
        }
        return null;
    }

}
