package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.ASMElement;
import edu.montana.cs.mtmc.asm.ASMError;
import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.asm.HasLocation;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction extends ASMElement {

    private final InstructionType type;
    private final MTMCToken instructionToken;

    public static boolean isInstruction(String cmd) {
        return InstructionType.fromString(cmd) != null;
    }

    public MTMCToken getInstructionToken() {
        return instructionToken;
    }

    public void validateLabel(Assembler assembler) {
        // default does nothing
    }

    public Instruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(label);
        this.type = type;
        this.instructionToken = instructionToken;
    }

    public void addError(String error) {
        addError(instructionToken, error);
    }

    public InstructionType getType() {
        return type;
    }

    public abstract void genCode(byte[] output, Assembler assembler);

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    public static String disassembleInstruction(short instruction) {
        String aluOp = ALUInstruction.disassemble(instruction);
        if (aluOp != null) {
            return aluOp;
        }
        String ldi = LoadImmediateInstruction.disassemble(instruction);
        if (ldi != null) {
            return ldi;
        }
        String jump = JumpInstruction.disassemble(instruction);
        if (jump != null) {
            return jump;
        }
        return "<unknown>";
    }
}
