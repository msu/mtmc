package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class JumpInstruction extends Instruction {

    private MTMCToken addressToken;

    public JumpInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setAddressToken(MTMCToken addressToken) {
        this.addressToken = addressToken;
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
            case JNZ -> opcode = 0b1110;
            case JAL -> opcode = 0b1111;
        }
        int address = resolveTargetAddress(assembler);
        output[getLocation()] = (byte) (opcode << 4 | address >>> 8);
        output[getLocation()+1] = (byte) address;
    }

    public static String disassemble(short instruction) {
        return null;
    }
}
