package edu.montana.cs.mtmc.asm;

public class InstructionBuilder {
    private InstructionType type;
    private String label;
    private String arg1;
    private String arg2;
    private String arg3;

    public InstructionBuilder setType(InstructionType type) {
        this.type = type;
        return this;
    }

    public InstructionBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public InstructionBuilder setArg1(String arg1) {
        this.arg1 = arg1;
        return this;
    }

    public InstructionBuilder setArg2(String arg2) {
        this.arg2 = arg2;
        return this;
    }

    public InstructionBuilder setArg3(String arg3) {
        this.arg3 = arg3;
        return this;
    }

    public Instruction createInstruction() {
        return new Instruction(type, label, arg1, arg2, arg3);
    }
}