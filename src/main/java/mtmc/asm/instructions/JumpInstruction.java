package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class JumpInstruction extends Instruction {

    public static final int MAX = (1 << 12) - 1;

    private MTMCToken addressToken;
    private MTMCToken register;

    public JumpInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setAddressToken(MTMCToken addressToken) {
        this.addressToken = addressToken;
    }

    public void setRegister(MTMCToken register) {
        this.register = register;
    }

    @Override
    public void validateLabel(Assembler assembler) {
        if (addressToken.type() == MTMCToken.TokenType.IDENTIFIER) {
            if (!assembler.hasLabel(addressToken.stringValue())) {
                addError("Unresolved label: " + addressToken.stringValue());
            }
        }
    }

    private Integer resolveTargetAddress(Assembler assembler) {
        if (addressToken.type() == MTMCToken.TokenType.IDENTIFIER) {
            return assembler.resolveLabel(addressToken.stringValue());
        } else {
            return addressToken.intValue();
        }
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opcode = 0;
        switch (getType()) {
            case J -> opcode = 0b1100;
            case JZ -> opcode = 0b1101;
            case JR -> opcode = 0b1110;
            case JAL -> opcode = 0b1111;
        }
        int address = resolveTargetAddress(assembler);
        if(getType() == InstructionType.JR) {
            output[getLocation()] = (byte) (opcode << 4);
            int reg = Register.toInteger(register.stringValue());
            output[getLocation()+1] = (byte) reg;
        } else {
            output[getLocation()] = (byte) (opcode << 4 | address >>> 8);
            output[getLocation()+1] = (byte) address;
        }
    }

    public static String disassemble(short instruction) {
        return null;
    }
}
