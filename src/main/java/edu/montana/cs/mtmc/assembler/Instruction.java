package edu.montana.cs.mtmc.assembler;

public class Instruction {

    private InstructionType type;
    private String label;
    private String arg1;
    private String arg2;
    private String arg3;

    public Instruction(InstructionType type, String label, String arg1, String arg2, String arg3) {
        this.type = type;
        this.label = label;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }
}
