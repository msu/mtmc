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
    private MTMCToken value;

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

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        if (getType() == InstructionType.SYS) {
            output[getLocation()] = 0b0000_0000;
            output[getLocation() + 1] = SysCall.getValue(this.syscallType.stringValue());
        } else if (getType() == InstructionType.MOV) {
            int to = Register.toInteger(toRegister.stringValue());
            int from = Register.toInteger(fromRegister.stringValue());
            output[getLocation()] = 0b0000_0001;
            output[getLocation() + 1] = (byte) (to << 4 | from);
        } else if (getType() == InstructionType.INC) {
            output[getLocation()] = 0b0000_0010;
            int to = Register.toInteger(toRegister.stringValue());
            int immediateVal = 1;
            if (value != null) {
                immediateVal = value.intValue();
            }
            output[getLocation() + 1] = (byte) (to << 4 | immediateVal);
        } else if (getType() == InstructionType.DEC) {
            output[getLocation()] = 0b0000_0100;
            output[getLocation() + 1] = toRegister.intValue().byteValue();
        } else if (getType() == InstructionType.NOP) {
            output[getLocation()] = 0b0000_1111;
            output[getLocation() + 1] = (byte) 0b1111_1111;
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

    public void setValue(MTMCToken value) {
        this.value = value;
    }
}
