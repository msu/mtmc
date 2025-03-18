package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class StackInstruction extends Instruction {

    public StackInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    private MTMCToken targetToken;
    private MTMCToken stackRegisterToken;
    private MTMCToken aluOpToken;

    @Override
    public void genCode(short[] output) {
        int stackReg = Registers.SP;
        if(stackRegisterToken != null) {
            stackReg = Registers.toInteger(stackRegisterToken.getStringValue());
        }
        if (getType() == InstructionType.PUSH) {
            int target = Registers.toInteger(targetToken.getStringValue());
            output[getLocation()] = (byte) (0b0010_0000_0000_0000 | target << 4 | stackReg);
        } else if (getType() == InstructionType.POP) {
            int target = Registers.toInteger(targetToken.getStringValue());
            output[getLocation()] = (byte) (0b0010_0001_0000_0000 | target << 4 | stackReg);
        } else if (getType() == InstructionType.SOP) {
            int aluOp = ALUInstruction.getALUOpcode(aluOpToken.getStringValue());
            output[getLocation()] = (byte) (0b0010_0100_0000_0000 | aluOp << 4 | stackReg);
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
            output[getLocation()] = (byte) (0b0010_0011_0000_0000 | stackOp << 4 | stackReg);
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
}
