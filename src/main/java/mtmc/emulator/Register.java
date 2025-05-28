package mtmc.emulator;

public enum Register {

    //=== user-facing registers
    T0,  // temp registers
    T1,
    T2,
    T3,
    T4,
    T5,
    A0,  // arg registers
    A1,
    A2,
    A3,
    RV,  // return value
    RA,  // return address
    FP, // frame pointer
    SP, // stack pointer
    BP, // break pointer
    PC,

    //=== non-user-facing registers
    IR,    // instruction register
    DR,    // data register
    CB,    // code boundary
    DB,    // data boundary
    IO,    // I/O register
    FLAGS; // flags register

    public static int toInteger(String reg) {
        return Register.valueOf(reg.toUpperCase()).ordinal();
    }

    public static String fromInteger(int reg) {
        return Register.values()[reg].name().toLowerCase();
    }

    public static boolean isWriteable(int reg) {
        return 0 <= reg && reg < 16;
    }

    public static boolean isReadable(int reg) {
        return 0 <= reg && reg < 16;
    }

    private static boolean isTempRegister(int reg) {
        return 0 <= reg && reg < 6;}

    public static boolean isWriteable(String register) {
        try {
            return isWriteable(toInteger(register));
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean isReadable(String register) {
        try {
            return isReadable(toInteger(register));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isTempRegister(String register) {
        try {
            return isTempRegister(toInteger(register));
        } catch (Exception e) {
            return false;
        }
    }

}
