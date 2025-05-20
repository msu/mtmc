package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

public class JumpRegisterInstruction extends Instruction {

    private MTMCToken register;

    public JumpRegisterInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setRegister(MTMCToken register) {
        this.register = register;
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opcode = 0b10001;
        output[getLocation()] = (byte) (opcode << 4);
        int reg = Register.toInteger(register.stringValue());
        output[getLocation()+1] = (byte) reg;
    }

    public static String disassemble(short instruction) {
        return null;
    }
}
