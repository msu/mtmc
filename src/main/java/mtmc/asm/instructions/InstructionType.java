package mtmc.asm.instructions;

import static mtmc.asm.instructions.InstructionType.InstructionClass.*;

public enum InstructionType {
    SYS(MISC),
    MOV(MISC),
    INC(MISC),
    DEC(MISC),
    NOP(MISC),
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
    MIN(ALU),
    MAX(ALU),
    NOT(ALU),
    LNOT(ALU),
    NEG(ALU),
    IMM(ALU, 4),
    PUSH(STACK),
    POP(STACK),
    DUP(STACK),
    SWAP(STACK),
    DROP(STACK),
    OVER(STACK),
    ROT(STACK),
    SOP(STACK),
    PUSHI(STACK, 4),
    EQ(TEST),
    NEQ(TEST),
    GT(TEST),
    GTE(TEST),
    LT(TEST),
    LTE(TEST),
    EQI(TEST),
    NEQI(TEST),
    GTI(TEST),
    GTEI(TEST),
    LTI(TEST),
    LTEI(TEST),
    LW(LOAD_STORE, 4),
    LWO(LOAD_STORE, 4),
    LI(LOAD_STORE, 4),
    LB(LOAD_STORE, 4),
    LBO(LOAD_STORE, 4),
    SW(LOAD_STORE, 4),
    SWO(LOAD_STORE, 4),
    SB(LOAD_STORE, 4),
    SBO(LOAD_STORE, 4),
    LWR(LOAD_STORE_RELATIVE),
    LBR(LOAD_STORE_RELATIVE),
    SWR(LOAD_STORE_RELATIVE),
    SBR(LOAD_STORE_RELATIVE),
    J(JUMP),
    JZ(JUMP),
    JR(JUMP),
    JAL(JUMP),
    ;


    public int getSizeInBytes() {
        return size;
    }

    public enum InstructionClass {
        MISC,
        ALU,
        STACK,
        TEST,
        LOAD_STORE,
        LOAD_STORE_RELATIVE,
        JUMP
    }

    private final InstructionClass instructionClass;
    private final int size;

    InstructionType(InstructionClass instructionClass) {
        this(instructionClass, 2);
    }

    InstructionType(InstructionClass instructionClass, int size) {
        this.instructionClass = instructionClass;
        this.size = size;
    }

    public InstructionClass getInstructionClass() {
        return instructionClass;
    }

    public static InstructionType fromString(String string) {
        return InstructionType.valueOf(string.toUpperCase());
    }
}
