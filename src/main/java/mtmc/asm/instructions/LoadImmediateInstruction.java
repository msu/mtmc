package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class LoadImmediateInstruction extends Instruction {

    public LoadImmediateInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public static final int MAX = (1 << 12) - 1;

    private MTMCToken tempRegisterToken;
    private MTMCToken valueToken;

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int reg = Register.toInteger(tempRegisterToken.stringValue());
        int value = resolveValue(assembler);
        output[getLocation()] = (byte) (0b1000_0000 | reg << 4 | value >>> 8);
        output[getLocation() + 1] = (byte) (value);
    }

    public void setTempRegister(MTMCToken stackRegister) {
        this.tempRegisterToken = stackRegister;
    }

    @Override
    public void validateLabel(Assembler assembler) {
        if (valueToken.type() == MTMCToken.TokenType.IDENTIFIER) {
            if (!assembler.hasLabel(valueToken.stringValue())) {
                addError("Unresolved label: " + valueToken.stringValue());
            }
        }
    }

    private Integer resolveValue(Assembler assembler) {
        if (valueToken.type() == MTMCToken.TokenType.IDENTIFIER) {
            return assembler.resolveLabel(valueToken.stringValue());
        } else {
            return valueToken.intValue();
        }
    }


    public void setValue(MTMCToken valueToken) {
        this.valueToken = valueToken;
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 2, instruction) == 0b10) {
            StringBuilder builder = new StringBuilder("ldi ");
            short reg = getBits(14, 2, instruction);
            builder.append(Register.fromInteger(reg)).append(" ");
            short val = getBits(12, 12, instruction);
            builder.append(val);
            return builder.toString();
        }
        return null;
    }

}
