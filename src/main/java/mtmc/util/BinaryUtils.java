package mtmc.util;

import org.jetbrains.annotations.NotNull;

public class BinaryUtils {

    /**
     * @param start - a base 1 index to start at (16 is the most significant, 1 is the least)
     * @param totalBits - the total bits to get
     * @param value - the value to get the bits from
     */
    public static short getBits(int start, int totalBits, short value) {
        if (totalBits <= 0) {
            return 0;
        }
        int returnValue = (value & 0xffff) >>> (start - totalBits);
        int mask = 0;
        int toShift = totalBits;
        while(toShift > 0) {
            toShift--;
            mask = mask << 1;
            mask = mask + 1;
        }
        return (short) (returnValue & mask);
    }

    @NotNull
    public static String toBinary(byte aByte) {
        String binaryString = Integer.toBinaryString(aByte);
        String formatted = String.format("%8s", binaryString);
        String zeroed = formatted.replaceAll(" ", "0");
        String underScored = zeroed.replaceAll("....", "$0_");
        String noTrailingUnderscore = underScored.substring(0, underScored.length() - 1);
        return "0b" + noTrailingUnderscore;
    }

    @NotNull
    public static String toBinary(short aShort) {
        String binaryString = Integer.toBinaryString(aShort);
        String formatted = String.format("%16s", binaryString);
        String zeroed = formatted.replaceAll(" ", "0");
        String underScored = zeroed.replaceAll("....", "$0_");
        String noTrailingUnderscore = underScored.substring(0, underScored.length() - 1);
        return "0b" + noTrailingUnderscore;
    }

    @NotNull
    public static String toBinary(int anInt) {
        String binaryString = Integer.toBinaryString(anInt);
        String formatted = String.format("%32s", binaryString);
        String zeroed = formatted.replaceAll(" ", "0");
        String underScored = zeroed.replaceAll("....", "$0_");
        String noTrailingUnderscore = underScored.substring(0, underScored.length() - 1);
        return "0b" + noTrailingUnderscore;
    }
}
