package edu.montana.cs.mtmc.emulator;

import java.lang.reflect.Field;

public class Registers {
    public static final int T0 = 0;
    public static final int T1 = 1;
    public static final int T2 = 2;
    public static final int T3 = 3;
    public static final int A0 = 4;
    public static final int A1 = 5;
    public static final int A2 = 6;
    public static final int A3 = 7;
    public static final int R0 = 8;
    public static final int RA = 9;
    public static final int FP = 10;
    public static final int SP = 11;
    public static final int BP = 12;
    public static final int PC = 13;
    public static final int ZERO = 14;
    public static final int ONE = 15;
    public static final int IR = 16;
    public static int toInteger(String reg) {
        try {
            Field field = Registers.class.getField(reg.toUpperCase());
            Integer index = (Integer) field.get(null);
            return index;
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isWriteable(int reg) {
        return 0 <= reg && reg < 14;
    }

    public static boolean isReadable(int reg) {
        return 0 <= reg && reg < 16;
    }

    private static boolean isTempRegister(int reg) {
        return 0 <= reg && reg < 4;}

    public static boolean isWriteable(String register) {
        int integer = toInteger(register);
        return isWriteable(integer);
    }
    public static boolean isReadable(String register) {
        int integer = toInteger(register);
        return isReadable(integer);
    }

    public static boolean isTempRegister(String register) {
        int integer = toInteger(register);
        return isTempRegister(integer);
    }

}
