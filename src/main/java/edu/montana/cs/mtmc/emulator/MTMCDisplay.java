package edu.montana.cs.mtmc.emulator;

import java.util.stream.IntStream;

public class MTMCDisplay {

    public static final String DARK = "2a453b";
    public static final String MEDIUM = "365d48";
    public static final String LIGHT = "577c44";
    public static final String WHITE = "7f860f";
    private final MonTanaMiniComputer computer;

    public MTMCDisplay(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public Iterable<Integer> getRows() {
        return () -> IntStream.range(0, 64).iterator();
    }

    public Iterable<Integer> getColumns() {
        return () -> IntStream.range(0, 64).iterator();
    }

    public short getValueFor(short x, short y) {
        int startingBit = 2 * ((x * 64) + y);
        int startingByte = MonTanaMiniComputer.FRAME_BUFF_START + startingBit / 8;
        int bitOffset = startingBit % 8;
        byte value = computer.fetchByteFromMemory(startingByte);
        int shiftedVal = value >>> bitOffset;
        int finalVal = shiftedVal & 0b011;
        return (short) finalVal;
    }

    public void setValueFor(short x, short y, short value) {
        value = (short) Math.min(Math.max(value, 0), 3); // cap value
        int startingBit = 2 * ((x * 64) + y);
        int startingByte = MonTanaMiniComputer.FRAME_BUFF_START + startingBit / 8;
        int bitOffset = startingBit % 8;
        int shiftedVal = value << bitOffset;
        int shiftedValMask = 0b11 << bitOffset;
        byte currentValue = computer.fetchByteFromMemory(startingByte);
        int resetCurrentValue = currentValue & ~shiftedValMask;
        int finalVal = resetCurrentValue | shiftedVal;
        computer.writeByteToMemory(startingByte, (byte) finalVal);
    }

    public void drawLine(short startX, short startY, short endX, short endY) {
        int diffX = endX - startX;
        int diffY = endY - startY;
        int steps = Math.max(Math.abs(diffX), Math.abs(diffY));
        int scaledX = startX * 64;
        int scaledY = startY * 64;
        int step = 0;
        while (step <= steps) {
            step++;
            int normalizedX = scaledX/64;
            int normalizedY = scaledY/64;
            if (0 <= normalizedX && normalizedX < 64 &&
                    0 <= normalizedY && normalizedY < 64) {
                setValueFor((short) normalizedX, (short) normalizedY, (short) 3);
            }
            scaledX += (diffX * 64) / steps;
            scaledY += (diffY * 64) / steps;
        }
    }
}
