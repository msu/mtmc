package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class StackImmediateInstruction extends Instruction {

    public StackImmediateInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public static final int MAX = (2 << 8) - 1;

    private MTMCToken stackRegisterToken;
    private MTMCToken valueToken;

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int stackReg = Register.SP.ordinal();
        if(stackRegisterToken != null) {
            stackReg = Register.toInteger(stackRegisterToken.stringValue());
        }
        int value = valueToken.intValue();
        output[getLocation()] = (byte) (0b0011_0000 | stackReg);
        output[getLocation() + 1] = (byte) value;
    }

    public void setStackRegister(MTMCToken stackRegister) {
        this.stackRegisterToken = stackRegister;
    }

    public void setValue(MTMCToken valueToken) {
        this.valueToken = valueToken;
    }

    public static String disassemble(short instruction) {
        if(getBits(16, 4, instruction) == 3) {
            short stackreg = getBits(12, 4, instruction);
            short value = getBits(8, 8, instruction);
            return "pushi " + value + Register.fromInteger(stackreg);
        }
        return null;
    }
}
