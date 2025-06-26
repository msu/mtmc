package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.emulator.Register;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

import static mtmc.util.BinaryUtils.getBits;

public class StackInstruction extends Instruction {

    private MTMCToken targetToken;
    private MTMCToken stackRegisterToken;
    private MTMCToken aluOpToken;
    private MTMCToken value;

    public StackInstruction(InstructionType type, List<MTMCToken> label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
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

    public void setValue(MTMCToken value) {
        this.value = value;
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int stackReg = Register.SP.ordinal();
        if(stackRegisterToken != null) {
            stackReg = Register.toInteger(stackRegisterToken.stringValue());
        }
        if (getType() == InstructionType.PUSH) {
            int target = Register.toInteger(targetToken.stringValue());
            output[getLocation()] = 0b0010_0000;
            output[getLocation() + 1] = (byte) (target << 4 | stackReg);
        } else if (getType() == InstructionType.POP) {
            int target = Register.toInteger(targetToken.stringValue());
            output[getLocation()] = 0b0010_0001;
            output[getLocation() + 1] = (byte) (target << 4 | stackReg);
        } else if (getType() == InstructionType.SOP) {
            int aluOp = ALUOp.toInteger(aluOpToken.stringValue());
            output[getLocation()] = 0b0010_0111;
            output[getLocation() + 1] = (byte) (aluOp << 4 | stackReg);
        } else if (getType() == InstructionType.PUSHI) {
            int immediateValue = value.intValue();
            output[getLocation()] = 0b0010_1111;
            output[getLocation() + 1] = (byte) stackReg;
            output[getLocation() + 2] = (byte) (immediateValue >>> 8);
            output[getLocation() + 3] = (byte) immediateValue;
        } else {
            int stackOp;
            stackOp = switch (getType()) {
                case DUP -> 0b0010_0010;
                case SWAP -> 0b0010_0011;
                case DROP -> 0b0010_0100;
                case OVER -> 0b0010_0101;
                case ROT -> 0b0010_0110;
                default -> 0b0000;
            };
            output[getLocation()] = (byte) stackOp;
            output[getLocation() + 1] = (byte) stackReg;
        }
    }

    public static String disassemble(short instruction) {
        if(getBits(16, 4, instruction) == 2) {
            short opCode = getBits(12, 4, instruction);
            short stackReg = getBits(4, 4, instruction);
            if (opCode == 0b0000) {
                short sourceReg = getBits(8, 4, instruction);
                return "push " + Register.fromInteger(sourceReg) + " " + Register.fromInteger(stackReg);
            }
            if (opCode == 0b0001) {
                short destReg = getBits(8, 4, instruction);
                return "pop " + Register.fromInteger(destReg) + " " + Register.fromInteger(stackReg);
            }
            if (opCode == 0b010) {
                return "dup " + Register.fromInteger(stackReg);
            } else if(opCode == 0b011) {
                return "swap " + Register.fromInteger(stackReg);
            } else if(opCode == 0b0100) {
                return "drop " + Register.fromInteger(stackReg);
            } else if(opCode == 0b0101) {
                return "over " + Register.fromInteger(stackReg);
            } else if(opCode == 0b0110) {
                return "rot " + Register.fromInteger(stackReg);
            } else if (opCode == 0b0111) {
                short aluOp = getBits(8, 4, instruction);
                String op = ALUOp.fromInt(aluOp);
                return "sop " + op + " " + Register.fromInteger(stackReg);
            } else if (opCode == 0b1111) {
                return "pushi " + Register.fromInteger(stackReg);
            }
        }
        return null;
    }

}
