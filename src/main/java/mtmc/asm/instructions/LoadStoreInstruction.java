package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class LoadStoreInstruction extends Instruction {

    private MTMCToken targetToken;
    private MTMCToken offsetToken;
    private MTMCToken value;

    public LoadStoreInstruction(InstructionType type, List<MTMCToken> label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setTargetToken(MTMCToken targetToken) {
        this.targetToken = targetToken;
    }

    public void setOffsetToken(MTMCToken offsetToken) {
        this.offsetToken = offsetToken;
    }

    public void setValue(MTMCToken value) {
        this.value = value;
    }

    public boolean isOffset() {
        return getType().name().endsWith("O");
    }

    @Override
    public void validateLabel(Assembler assembler) {
        if (value.type() == MTMCToken.TokenType.IDENTIFIER) {
            if (!assembler.hasLabel(value.stringValue())) {
                addError("Unresolved label: " + value.stringValue());
            }
        }
    }

    private Integer resolveValue(Assembler assembler) {
        if (value.type() == MTMCToken.TokenType.IDENTIFIER) {
            return assembler.resolveLabel(value.stringValue());
        } else {
            return value.intValue();
        }
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int upperByte = switch (getType()) {
            case LW -> 0b1000_0000;
            case LWO -> 0b1000_0001;
            case LB -> 0b1000_0010;
            case LBO -> 0b1000_0011;
            case SW -> 0b1000_0100;
            case SWO -> 0b1000_0101;
            case SB -> 0b1000_0110;
            case SBO -> 0b1000_0111;
            case LI -> 0b1000_1111;
            default -> 0;
        };

        int target = Register.toInteger(targetToken.stringValue());
        output[getLocation()] = (byte) upperByte;

        if (isOffset()) {
            int offsetReg = Register.toInteger(offsetToken.stringValue());
            output[getLocation() + 1] = (byte) (target << 4 | offsetReg);
        } else {
            output[getLocation() + 1] = (byte) (target << 4);
        }

        int numericValue = resolveValue(assembler);
        output[getLocation() + 2] = (byte) (numericValue >>> 8);
        output[getLocation() + 3] = (byte) numericValue;

    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 0b1000) {
            short topNibble = getBits(12, 4, instruction);
            StringBuilder sb = new StringBuilder();
            if (topNibble == 0b1111) {
                sb.append("li ");
            } else if (topNibble == 0b000) {
                sb.append("lw ");
            } else if (topNibble == 0b001) {
                sb.append("lwo ");
            } else if (topNibble == 0b010) {
                sb.append("lb ");
            } else if (topNibble == 0b011) {
                sb.append("lbo ");
            } else if (topNibble == 0b100) {
                sb.append("sw ");
            } else if (topNibble == 0b101) {
                sb.append("swo ");
            } else if (topNibble == 0b110) {
                sb.append("sb ");
            } else if (topNibble == 0b111) {
                sb.append("sbo ");
            }
            short target = getBits(8, 4, instruction);
            String reg = Register.fromInteger(target);
            sb.append(reg);
            if (topNibble == 0b001 || topNibble == 0b011 || topNibble == 0b101 || topNibble == 0b111) {
                short offset = getBits(4, 4, instruction);
                String offsetReg = Register.fromInteger(offset);
                sb.append(" ").append(offsetReg);
            }
            return sb.toString();
        }
        return null;
    }

}
