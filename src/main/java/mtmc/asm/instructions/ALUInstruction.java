package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class ALUInstruction extends Instruction {

    private MTMCToken toToken;
    private MTMCToken fromToken;

    public ALUInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setTo(MTMCToken to) {
        this.toToken =  to;
    }

    public void setFrom(MTMCToken from) {
        this.fromToken = from;
    }

    public boolean isBinaryOp() {
        return !ALUOp.valueOf(getInstructionToken().stringValue().toUpperCase()).isUnary();
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opCode = ALUOp.toInteger(getInstructionToken().stringValue());
        int to = Register.toInteger(toToken.stringValue());
        int from = 0;
        if (fromToken != null) {
            from = Register.toInteger(fromToken.stringValue());
        }
        output[getLocation()] = (byte) (0b0001_0000 | opCode);
        output[getLocation() + 1] = (byte) (to << 4 | from);
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 1) {
            StringBuilder builder = new StringBuilder();
            short opCode = getBits(12, 4, instruction);
            String op = ALUOp.fromInt(opCode);
            builder.append(op).append(" ");
            builder.append(Register.fromInteger(getBits(8, 4, instruction))).append(" ");
            builder.append(Register.fromInteger(getBits(4, 4, instruction))).append(" ");
            return builder.toString();
        }
        return null;
    }

}
