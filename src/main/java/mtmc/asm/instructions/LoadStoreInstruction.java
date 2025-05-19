package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class LoadStoreInstruction extends Instruction {

    private MTMCToken targetToken;
    private MTMCToken offsetToken;
    private MTMCToken value;

    public LoadStoreInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
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
            case LW -> 0b0100_0000;
            case LWO -> 0b0100_0001;
            case LB -> 0b0100_0010;
            case LBO -> 0b0100_0011;
            case SW -> 0b0100_0100;
            case SWO -> 0b0100_0101;
            case SB -> 0b0100_0110;
            case SBO -> 0b0100_0111;
            case LI -> 0b0100_1111;
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
        return null;
    }

}
