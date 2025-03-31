package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Registers;
import mtmc.tokenizer.MTMCToken;

import static mtmc.util.BinaryUtils.getBits;

public class StackInstruction extends Instruction {

    public StackInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    private MTMCToken targetToken;
    private MTMCToken stackRegisterToken;
    private MTMCToken aluOpToken;

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int stackReg = Registers.SP;
        if(stackRegisterToken != null) {
            stackReg = Registers.toInteger(stackRegisterToken.stringValue());
        }
        if (getType() == InstructionType.PUSH) {
            int target = Registers.toInteger(targetToken.stringValue());
            output[getLocation()] = 0b0010_0000;
            output[getLocation() + 1] = (byte) (target << 4 | stackReg);
        } else if (getType() == InstructionType.POP) {
            int target = Registers.toInteger(targetToken.stringValue());
            output[getLocation()] = 0b0010_0001;
            output[getLocation() + 1] = (byte) (target << 4 | stackReg);
        } else if (getType() == InstructionType.SOP) {
            int aluOp = ALUInstruction.getALUOpcode(aluOpToken.stringValue());
            output[getLocation()] = 0b0010_0011;
            output[getLocation() + 1] = (byte) (aluOp << 4 | stackReg);
        } else {
            int stackOp;
            switch (getType()) {
                case DUP -> stackOp = 0b0000;
                case SWAP -> stackOp = 0b0001;
                case DROP -> stackOp = 0b0010;
                case OVER -> stackOp = 0b0011;
                case ROT -> stackOp = 0b0100;
                case null, default -> stackOp = 0b0000;
            }
            output[getLocation()] = 0b0010_0010;
            output[getLocation()] = (byte) (stackOp << 4 | stackReg);
        }
    }

    public void setTarget(MTMCToken target) {
        this.targetToken = target;
    }

    public void setStackRegister(MTMCToken stackRegister) {
        this.stackRegisterToken = stackRegister;
    }

    public void setALUOp(MTMCToken aluOp) {
        this.aluOpToken = aluOp;
    }

    public static String disassemble(short instruction) {
        if(getBits(16, 4, instruction) == 2) {
            short type = getBits(12, 4, instruction);
            if (type == 0) {
                short sourceReg = getBits(8, 4, instruction);
                short stackReg = getBits(4, 4, instruction);
                return "push " + Registers.fromInteger(sourceReg) + " " + Registers.fromInteger(stackReg);
            }
            if (type == 1) {
                short destReg = getBits(8, 4, instruction);
                short stackReg = getBits(4, 4, instruction);
                return "pop " + Registers.fromInteger(destReg) + " " + Registers.fromInteger(stackReg);
            }
            if (type == 2) {
                short opcode = getBits(8, 4, instruction);
                short stackReg = getBits(4, 4, instruction);
                if (opcode == 0) {
                    return "dup " + Registers.fromInteger(stackReg);
                } else if(opcode == 1) {
                    return "swap " + Registers.fromInteger(stackReg);
                } else if(opcode == 2) {
                    return "drop " + Registers.fromInteger(stackReg);
                } else if(opcode == 3) {
                    return "over " + Registers.fromInteger(stackReg);
                } else if(opcode == 4) {
                    return "rot " + Registers.fromInteger(stackReg);
                }
            }
            if (type == 3) {
                short opcode = getBits(8, 4, instruction);
                short stackReg = getBits(4, 4, instruction);
                String op = ALUInstruction.getALUOp(opcode);
                return "sop " + op + " " + Registers.fromInteger(stackReg);
            }
        }
        return null;
    }

}
