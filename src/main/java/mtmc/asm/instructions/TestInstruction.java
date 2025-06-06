package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class TestInstruction extends Instruction {

    private MTMCToken first;
    private MTMCToken second;
    private MTMCToken value;

    public TestInstruction(InstructionType type, List<MTMCToken> label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setFirst(MTMCToken to) {
        this.first =  to;
    }

    public void setSecond(MTMCToken from) {
        this.second = from;
    }

    public void setImmediateValue(MTMCToken value) {
        this.value = value;
    }

    public boolean isImmediate() {
        return getType().name().endsWith("I");
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int firstReg = Register.toInteger(first.stringValue());
        if (isImmediate()) {
            int upperByte = switch (getType()) {
                case EQI -> 0b0011_1000;
                case NEQI -> 0b0011_1001;
                case GTI -> 0b0011_1010;
                case GTEI -> 0b0011_1011;
                case LTI -> 0b0011_1100;
                case LTEI -> 0b0011_1101;
                default -> 0;
            };
            int immediateValue = value.intValue();
            output[getLocation()] = (byte) upperByte;
            output[getLocation() + 1] = (byte) (firstReg << 4 | immediateValue);
        } else {
            int upperByte = switch (getType()) {
                case EQ -> 0b0011_0000;
                case NEQ -> 0b0011_0001;
                case GT -> 0b0011_0010;
                case GTE -> 0b0011_0011;
                case LT -> 0b0011_0100;
                case LTE -> 0b0011_0101;
                default -> 0;
            };
            int secondReg = Register.toInteger(second.stringValue());
            output[getLocation()] = (byte) upperByte;
            output[getLocation() + 1] = (byte) (firstReg << 4 | secondReg);
        }
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 0b0011) {
            StringBuilder sb = new StringBuilder();
            short opCode = getBits(12, 4, instruction);
            short thirdNibble = getBits(8, 4, instruction);
            short fourthNibble = getBits(4, 4, instruction);
            if (opCode == 0b0000) {
                sb.append("eq ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(Register.fromInteger(fourthNibble));
            } else if (opCode == 0b0001) {
                sb.append("neq ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(Register.fromInteger(fourthNibble));
            } else if (opCode == 0b0010) {
                sb.append("gt ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(Register.fromInteger(fourthNibble));
            } else if (opCode == 0b0011) {
                sb.append("gte ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(Register.fromInteger(fourthNibble));
            } else if (opCode == 0b0100) {
                sb.append("lt ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(Register.fromInteger(fourthNibble));
            } else if (opCode == 0b0101) {
                sb.append("lte ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(Register.fromInteger(fourthNibble));
            }
            if (opCode == 0b1000) {
                sb.append("eqi ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(fourthNibble);
            } else if (opCode == 0b1001) {
                sb.append("neqi ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(fourthNibble);
            } else if (opCode == 0b1010) {
                sb.append("gti ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(fourthNibble);
            } else if (opCode == 0b1011) {
                sb.append("gtei ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(fourthNibble);
            } else if (opCode == 0b1100) {
                sb.append("lti ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(fourthNibble);
            } else if (opCode == 0b1101) {
                sb.append("ltei ");
                sb.append(Register.fromInteger(thirdNibble))
                        .append(" ").append(fourthNibble);
            }
            return sb.toString();
        }
        return null;
    }

}
