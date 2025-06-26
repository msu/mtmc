package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class JumpRegisterInstruction extends Instruction {

    private MTMCToken register;

    public JumpRegisterInstruction(InstructionType type, List<MTMCToken> label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setRegister(MTMCToken register) {
        this.register = register;
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opcode = 0b1001;
        output[getLocation()] = (byte) (opcode << 4);
        int reg = Register.toInteger(register.stringValue());
        output[getLocation()+1] = (byte) reg;
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 5, instruction) == 0b1001) {
            short reg = getBits(4, 4, instruction);
            StringBuilder sb = new StringBuilder("jr");
            sb.append(Register.fromInteger(reg));
        }
        return null;
    }
}
