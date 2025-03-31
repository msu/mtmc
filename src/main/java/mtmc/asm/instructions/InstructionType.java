package mtmc.asm.instructions;

import static mtmc.asm.instructions.InstructionType.InstructionClass.*;

public enum InstructionType {
    SYS(MISC),
    MV(MISC),
    NOOP(MISC),
    ADD(ALU),
    SUB(ALU),
    MUL(ALU),
    DIV(ALU),
    MOD(ALU),
    AND(ALU),
    OR(ALU),
    XOR(ALU),
    SHL(ALU),
    SHR(ALU),
    EQ(ALU),
    LT(ALU),
    LTEQ(ALU),
    BNOT(ALU),
    NOT(ALU),
    NEG(ALU),
    PUSH(STACK),
    POP(STACK),
    DUP(STACK),
    SWAP(STACK),
    DROP(STACK),
    OVER(STACK),
    ROT(STACK),
    SOP(STACK),
    PUSHI(STACK_IMMEDIATE),
    LW(LOAD),
    LB(LOAD),
    SW(LOAD),
    SB(LOAD),
    LDI(LOAD_IMMEDIATE),
    J(JUMP),
    JZ(JUMP),
    JNZ(JUMP),
    JAL(JUMP),
    ;

    public enum InstructionClass {
        MISC,
        ALU,
        STACK,
        STACK_IMMEDIATE,
        LOAD,
        LOAD_IMMEDIATE,
        JUMP
    }

    private final String string;
    private final InstructionClass instructionClass;

    InstructionType(InstructionClass instructionClass) {
        this.string = this.name().toLowerCase();
        this.instructionClass = instructionClass;
    }

    public InstructionClass getInstructionClass() {
        return instructionClass;
    }

    public static InstructionType fromString(String string) {
        return InstructionType.valueOf(string.toUpperCase());
    }
}
