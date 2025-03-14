package edu.montana.cs.mtmc.emulator;

import java.util.stream.IntStream;

public class MTMCDisplay {

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

    public short getValueFor(short row, short column) {
        int startingBit = 2 * ((row * 64) + column);
        int startingByte = MonTanaMiniComputer.FRAME_BUFF_START + startingBit / 8;
        int bitOffset = startingBit % 8;
        byte value = computer.fetchByte(startingByte);
        int shiftedVal = value >>> bitOffset;
        int finalVal = shiftedVal & 0b011;
        return (short) finalVal;
    }

    public void setValueFor(short row, short column, short value) {
        value = (short) Math.min(Math.max(value, 0), 3); // cap value
        int startingBit = 2 * ((row * 64) + column);
        int startingByte = MonTanaMiniComputer.FRAME_BUFF_START + startingBit / 8;
        int bitOffset = startingBit % 8;
        int shiftedVal = value << bitOffset;
        int shiftedValMask = 0b11 << bitOffset;
        byte currentValue = computer.fetchByte(startingByte);
        int resetCurrentValue = currentValue & ~shiftedValMask;
        int finalVal = resetCurrentValue | shiftedVal;
        computer.writeByte(startingByte, (byte) finalVal);
    }
}
