package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.os.SysCall;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class MiscInstruction extends Instruction {

    private MTMCToken syscallType;
    private MTMCToken fromRegister;
    private MTMCToken toRegister;
    private MTMCToken shiftOrMask;

    public MiscInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
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

    public void setShiftOrMask(MTMCToken shiftOrMask) {
        this.shiftOrMask = shiftOrMask;
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        if (getType() == InstructionType.NOOP) {
            output[getLocation()] = 0;
            output[getLocation() + 1] = 0;
        } else if (getType() == InstructionType.SYS) {
            output[getLocation()] = 0b0000_1111;
            output[getLocation() + 1] = SysCall.getValue(this.syscallType.stringValue());
        } else if (getType() == InstructionType.MV) {
            int to = Register.toInteger(toRegister.stringValue());
            int from = Register.toInteger(fromRegister.stringValue());
            int shiftVal = 0;
            if(shiftOrMask != null) {
                shiftVal = shiftOrMask.intValue();
            }
            output[getLocation()] = (byte) to;
            output[getLocation() + 1] = (byte) (from << 4 | shiftVal);
        } else if (getType() == InstructionType.MASK) {
            output[getLocation()] = 0b0000_1110;
            output[getLocation() + 1] = shiftOrMask.intValue().byteValue();
        }
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 0) {
            short topNibble = getBits(12, 4, instruction);
            if (topNibble == 0b1111) {
                StringBuilder builder = new StringBuilder("sys ");
                short bits = getBits(8, 8, instruction);
                String name = SysCall.getString((byte) bits);
                builder.append(name);
                return builder.toString();
            } else if (topNibble == 0b1110) {
                StringBuilder builder = new StringBuilder("mask ");
                short bits = getBits(8, 8, instruction);
                builder.append(bits);
                return builder.toString();
            } else {
                short to = topNibble;
                short from = getBits(8, 4, instruction);
                short shift = getBits(4, 4, instruction);
                if(to == 0 && from == 0 && shift == 0) {
                    return "noop";
                }

                StringBuilder builder = new StringBuilder("mv ");
                builder.append(Register.fromInteger(to)).append(" ");
                builder.append(Register.fromInteger(from)).append(" ");
                if(shift != 0) {
                    builder.append(shift);
                }
                return builder.toString();
            }
        }
        return null;
    }

}
