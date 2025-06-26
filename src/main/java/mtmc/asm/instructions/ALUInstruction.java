package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class ALUInstruction extends Instruction {

    private MTMCToken toToken;
    private MTMCToken fromToken;
    private MTMCToken immediateOp;
    private MTMCToken value;

    public ALUInstruction(InstructionType type, List<MTMCToken> label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    public void setTo(MTMCToken to) {
        this.toToken =  to;
    }

    public void setFrom(MTMCToken from) {
        this.fromToken = from;
    }

    public void setImmediateValue(MTMCToken value) {
        this.value = value;
    }

    public void setImmediateOp(MTMCToken immediateOp) {
        this.immediateOp = immediateOp;
    }

    public boolean isImmediateOp() {
        return this.getType() == InstructionType.IMM;
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
        if (isBinaryOp()) {
            output[getLocation() + 1] = (byte) (to << 4 | from);
        } else if (isImmediateOp()) {
            int immediateValue = value.intValue();
            int immediateOpValue = ALUOp.toInteger(immediateOp.stringValue());
            output[getLocation() + 1] = (byte) (to << 4 | immediateOpValue);
            output[getLocation() + 2] = (byte) (immediateValue >>> 8);
            output[getLocation() + 3] = (byte) immediateValue;
        } else { // unary op
            output[getLocation() + 1] = (byte) (to << 4);
        }
    }

    public static String disassemble(short instruction) {
        if (getBits(16, 4, instruction) == 1) {
            StringBuilder builder = new StringBuilder();
            short opCode = getBits(12, 4, instruction);
            String op = ALUOp.fromInt(opCode);
            ALUOp aluOp = ALUOp.valueOf(op.toUpperCase());
            if (aluOp == ALUOp.IMM) {
                builder.append(op).append(" ");
                builder.append(Register.fromInteger(getBits(8, 4, instruction))).append(" ");
                builder.append(ALUOp.fromInt(getBits(4, 4, instruction))).append(" ");
            } else if (aluOp.isUnary()) {
                builder.append(op).append(" ");
                builder.append(Register.fromInteger(getBits(8, 4, instruction))).append(" ");
            } else {
                builder.append(op).append(" ");
                builder.append(Register.fromInteger(getBits(8, 4, instruction))).append(" ");
                builder.append(Register.fromInteger(getBits(4, 4, instruction))).append(" ");
            }
            return builder.toString();
        }
        return null;
    }

}
