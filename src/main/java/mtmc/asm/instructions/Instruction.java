package mtmc.asm.instructions;

import mtmc.asm.ASMElement;
import mtmc.asm.Assembler;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.tokenizer.MTMCToken;

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

    public Instruction(InstructionType type, List<MTMCToken> labels, MTMCToken instructionToken) {
        super(labels, instructionToken.line());
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
        return type == null ? 0 : type.getSizeInBytes();
    }

    public static String disassemble(short instruction, short previousInstruction) {
        if (MonTanaMiniComputer.isDoubleWordInstruction(previousInstruction)) {
            return String.valueOf(instruction);
        }
        String misc = MiscInstruction.disassemble(instruction);
        if (misc != null) {
            return misc;
        }
        String aluOp = ALUInstruction.disassemble(instruction);
        if (aluOp != null) {
            return aluOp;
        }
        String stack = StackInstruction.disassemble(instruction);
        if (stack != null) {
            return stack;
        }
        String test = TestInstruction.disassemble(instruction);
        if (test != null) {
            return test;
        }
        String lsr = LoadStoreRegisterInstruction.disassemble(instruction);
        if (lsr != null) {
            return lsr;
        }
        String ls = LoadStoreInstruction.disassemble(instruction);
        if (ls != null) {
            return ls;
        }
        String jumpReg = JumpInstruction.disassemble(instruction);
        if (jumpReg != null) {
            return jumpReg;
        }
        String jump = JumpInstruction.disassemble(instruction);
        if (jump != null) {
            return jump;
        }
        return "<unknown>";
    }
}
