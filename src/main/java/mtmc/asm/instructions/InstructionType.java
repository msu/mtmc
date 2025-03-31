package mtmc.asm.instructions;

import static mtmc.asm.instructions.InstructionType.InstructionClass.*;

public enum InstructionType {
    SYS("sys", MISC),
    MV("mv", MISC),
    NOOP("noop", MISC),
    ADD("add", ALU),
    SUB("sub", ALU),
    MUL("mul", ALU),
    DIV("div", ALU),
    MOD("mod", ALU),
    AND("and", ALU),
    OR("or", ALU),
    XOR("xor", ALU),
    SHL("shl", ALU),
    SHR("shr", ALU),
    EQ("eq", ALU),
    LT("lt", ALU),
    LTEQ("lteq", ALU),
    BNOT("bnot", ALU),
    NOT("not", ALU),
    NEG("neg", ALU),
    PUSH("push", STACK),
    POP("pop", STACK),
    DUP("dup", STACK),
    SWAP("swap", STACK),
    DROP("drop", STACK),
    OVER("over", STACK),
    ROT("rot", STACK),
    SOP("sop", STACK),
    PUSHI("pushi", STACK_IMMEDIATE),
    LW("lw", LOAD),
    LB("lb", LOAD),
    SW("sw", LOAD),
    SB("sb", LOAD),
    LDI("ldi", LOAD_IMMEDIATE),
    J("j", JUMP),
    JZ("jz", JUMP),
    JNZ("jnz", JUMP),
    JAL("jal", JUMP),
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

    InstructionType(String string, InstructionClass instructionClass) {
        this.string = string;
        this.instructionClass = instructionClass;
    }

    public InstructionClass getInstructionClass() {
        return instructionClass;
    }

    public static InstructionType fromString(String string) {
        for (InstructionType type : InstructionType.values()) {
            if (type.string.equals(string)) {
                return type;
            }
        }
        return null;
    }
}
