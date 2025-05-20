package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class TestInstruction extends Instruction {

    private MTMCToken first;
    private MTMCToken second;
    private MTMCToken value;

    public TestInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
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
        return null;
    }

}
