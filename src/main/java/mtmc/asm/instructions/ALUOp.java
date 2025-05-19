package mtmc.asm.instructions;

public enum ALUOp {
    ADD(0x0000, false),
    SUB(0x0001, false),
    MUL(0x0002, false),
    DIV(0x0003, false),
    MOD(0x0004, false),
    AND(0x0005, false),
    OR(0x0006, false),
    XOR(0x0007, false),
    SHL(0x0008, false),
    SHR(0x0009, false),
    MIN(0x000A, false),
    MAX(0x000B, false),
    NOT(0x000C, true),
    LNOT(0x000D, true),
    NEG(0x000E, true),
    IMM(0x000F, true);

    int opCode;
    boolean unary;

    ALUOp(int value, boolean unary) {
        this.opCode = value;
        this.unary = unary;
    }

    public static int toInteger(String instruction) {
        return valueOf(instruction.toUpperCase()).opCode;
    }

    public static String fromInt(short opCode) {
        return values()[opCode].name().toLowerCase();
    }

    public static boolean isALUOp(String op) {
        try {
            ALUOp aluOp = ALUOp.valueOf(op.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public int getOpCode() {
        return opCode;
    }

    public boolean isUnary() {
        return unary;
    }
}
