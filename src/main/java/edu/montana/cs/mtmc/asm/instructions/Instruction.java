package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.tokenizer.MTMCToken;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction {

    private final MTMCToken label;
    private final InstructionType type;
    private final MTMCToken instructionToken;
    List<Error> errors = new ArrayList<>();
    private int location = -1;

    public static boolean isInstruction(String cmd) {
        return InstructionType.fromString(cmd) != null;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public MTMCToken getInstructionToken() {
        return instructionToken;
    }

    public record Error(MTMCToken token, String error) {}

    public Instruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        this.label = label;
        this.type = type;
        this.instructionToken = instructionToken;
    }

    public void addError(String error) {
        addError(instructionToken, error);
    }
    public void addError(MTMCToken token, String error) {
        errors.add(new Error(token, error));
    }

    public MTMCToken getLabel() {
        return label;
    }

    public InstructionType getType() {
        return type;
    }

    public abstract void genCode(short[] output);
}
