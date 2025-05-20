package mtmc.emulator;

import java.util.stream.IntStream;

public class MTMCDisplay {

    public static final String DARK = "2a453b";
    public static final String MEDIUM = "365d48";
    public static final String LIGHT = "577c44";
    public static final String WHITE = "7f860f";
    public static final int ROWS = 128;
    public static final int COLS = 128;
    private final MonTanaMiniComputer computer;

    public MTMCDisplay(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public Iterable<Integer> getRows() {
        return () -> IntStream.range(0, ROWS).iterator();
    }

    public Iterable<Integer> getColumns() {
        return () -> IntStream.range(0, COLS).iterator();
    }

    public short getValueFor(short x, short y) {
        int startingBit = 2 * ((x * ROWS) + y);
        int startingByte = MonTanaMiniComputer.FRAME_BUFF_START + startingBit / 8;
        int bitOffset = startingBit % 8;
        byte value = computer.fetchByteFromMemory(startingByte);
        int shiftedVal = value >>> bitOffset;
        int finalVal = shiftedVal & 0b011;
        return (short) finalVal;
    }

    public void setValueFor(short x, short y, short value) {
        value = (short) Math.min(Math.max(value, 0), 3); // cap value
        int startingBit = 2 * ((x * ROWS) + y);
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
        int scaledX = startX * COLS;
        int scaledY = startY * ROWS;
        int step = 0;
        while (step <= steps) {
            step++;
            int normalizedX = scaledX/ROWS;
            int normalizedY = scaledY/COLS;
            if (0 <= normalizedX && normalizedX < ROWS &&
                    0 <= normalizedY && normalizedY < COLS) {
                setValueFor((short) normalizedX, (short) normalizedY, (short) 3);
            }
            scaledX += (diffX * ROWS) / steps;
            scaledY += (diffY * COLS) / steps;
        }
    }
}
