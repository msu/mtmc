package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.os.SysCall;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class MiscInstruction extends Instruction {

    private MTMCToken syscallType;
    private MTMCToken fromRegister;
    private MTMCToken toRegister;
    private MTMCToken value;

    public MiscInstruction(InstructionType type, List<MTMCToken> labels, MTMCToken instructionToken) {
        super(type, labels, instructionToken);
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

    public void setValue(MTMCToken value) {
        this.value = value;
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
            output[getLocation()] = 0b0000_0011;
            int to = Register.toInteger(toRegister.stringValue());
            int immediateVal = 1;
            if (value != null) {
                immediateVal = value.intValue();
            }
            output[getLocation() + 1] = (byte) (to << 4 | immediateVal);
        } else if (getType() == InstructionType.SETI) {
            output[getLocation()] = 0b0000_0100;
            int to = Register.toInteger(toRegister.stringValue());
            int immediateVal = value.intValue();
            output[getLocation() + 1] = (byte) (to << 4 | immediateVal);
        } else if (getType() == InstructionType.MCP) {
            output[getLocation()] = 0b000_0101;
            int from = Register.toInteger(fromRegister.stringValue());
            int to = Register.toInteger(toRegister.stringValue());
            int value = this.value.intValue();
            output[getLocation() + 1] = (byte) (from << 4 | to);
            output[getLocation() + 2] = (byte) (value << 8);
            output[getLocation() + 3] = (byte) (value & 0xFF);
        } else if (getType() == InstructionType.DEBUG) {
            output[getLocation()] = 0b0000_1000;
            output[getLocation() + 1] = value.intValue().byteValue();
        } else if (getType() == InstructionType.NOP) {
            output[getLocation()] = 0b0000_1111;
            output[getLocation() + 1] = (byte) 0b1111_1111;
        }
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 0) {
            short topNibble = getBits(12, 4, instruction);
            if (topNibble == 0b0000) {
                StringBuilder builder = new StringBuilder("sys ");
                short bits = getBits(8, 8, instruction);
                String name = SysCall.getString((byte) bits);
                builder.append(name);
                return builder.toString();
            } else if (topNibble == 0b0001) {
                StringBuilder builder = new StringBuilder("mov ");
                short to = getBits(8, 4, instruction);
                short from = getBits(4, 4, instruction);
                String toName = Register.fromInteger(to);
                builder.append(toName).append(" ");
                String fromName = Register.fromInteger(from);
                builder.append(fromName);
                return builder.toString();
            } else if (topNibble == 0b0010) {
                StringBuilder builder = new StringBuilder("inc ");
                short to = getBits(8, 4, instruction);
                short amount = getBits(4, 4, instruction);
                String toName = Register.fromInteger(to);
                builder.append(toName).append(" ");
                builder.append(amount);
                return builder.toString();
            } else if (topNibble == 0b0011) {
                StringBuilder builder = new StringBuilder("dec ");
                short to = getBits(8, 4, instruction);
                short amount = getBits(4, 4, instruction);
                String toName = Register.fromInteger(to);
                builder.append(toName).append(" ");
                builder.append(amount);
                return builder.toString();
            } else if (topNibble == 0b0100) {
                StringBuilder builder = new StringBuilder("seti ");
                short to = getBits(8, 4, instruction);
                short amount = getBits(4, 4, instruction);
                String toName = Register.fromInteger(to);
                builder.append(toName).append(" ");
                builder.append(amount);
                return builder.toString();
            } else if (topNibble == 0b0101) {
                short from = getBits(8, 4, instruction);
                String fromName = Register.fromInteger(from);
                short to = getBits(4, 4, instruction);
                String toName = Register.fromInteger(to);
                return "mcp " + fromName + " " + toName;
            } else if (topNibble == 0b1000) {
                StringBuilder builder = new StringBuilder("debug ");
                short stringIndex = getBits(8, 8, instruction);
                builder.append(stringIndex);
                return builder.toString();
            } else if (topNibble == 0b1111) {
                StringBuilder builder = new StringBuilder("noop");
                return builder.toString();
            }
        }
        return null;
    }

}
