package edu.montana.cs.mtmc.web;

import edu.montana.cs.mtmc.emulator.MTMCDisplay;
import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.emulator.Registers;
import kotlin.text.Charsets;

import java.util.stream.IntStream;

public class MTMCWebView {

    private final MonTanaMiniComputer computer;

    private DisplayFormat registerFormat = DisplayFormat.HEX;
    private DisplayFormat memoryFormat = DisplayFormat.HEX;

    public MTMCWebView(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public Iterable getMemoryAddresses(){
        return computer.getMemoryAddresses();
    }

    public Iterable blinkenIndexes() {
        return () -> IntStream.range(0, 16).iterator();
    }

    public String blinken(int bit, int register) {
        short value = computer.getRegister(register);
        int mask = 0b1 << bit;
        value = (short) (value & mask);
        if (value == 0) {
            return "off";
        } else {
            return "on";
        }
    }

    public String regDisplay(String reg) {
        try {
            short value = getRegisterValue(reg);
            switch (registerFormat) {
                case HEX -> {
                    String str = Integer.toHexString(value & 0xffff);
                    String padded = "%1$4s".formatted(str).replace(" ", "0");
                    return "0x" + padded;
                }
                case DEC -> {
                    return "" + value;
                }
                case ASCII -> {
                    return new String(new byte[]{(byte) (value >> 8), (byte) value});
                }
                case null, default -> {
                    return "<error>";
                }
            }
        } catch (Exception e) {
            return "No such register: " + reg;
        }
    }

    public String regBlinken(String reg) {
        Integer regIndex = Registers.toInteger(reg);
        StringBuilder blinken = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            blinken.append("<div class='blinken ");
            boolean space = i != 0 && (i + 1) % 4 == 0;
            if (space) {
                blinken.append(" space ");
            }
            blinken.append(blinken(15 - i, regIndex)).append("'></div>");

        }
        return blinken.toString();
    }

    private short getRegisterValue(String reg) {
        Integer index = Registers.toInteger(reg);
        short register = computer.getRegister(index);
        return register;
    }


    public String classFor(int address) {
        if (address >= MonTanaMiniComputer.FRAME_BUFF_START) {
            return "frameBuffer";
        } else if (address >= computer.getRegister(Registers.SP)) {
            return "stack";
        } else if (address == computer.getRegister(Registers.PC)) {
            return "currentInst";
        } else if (address <= computer.getRegister(Registers.CB)) {
            return "code";
        } else if (address <= computer.getRegister(Registers.DB)) {
            return "data";
        } else if (address <= computer.getRegister(Registers.BP)) {
            return "heap";
        } else {
            return "";
        }
    }

    public String displayValue(int address) {
        if (memoryFormat == DisplayFormat.HEX) {
            byte byteVal = computer.fetchByte(address);
            return String.format("%02X ", byteVal);
        } else if (memoryFormat == DisplayFormat.DEC) {
            if (address % 2 == 1) {
                return "" + computer.fetchWord(address - 1);
            } else {
                return "";
            }
        } else {
            byte value = computer.fetchByte(address);
            return new String(new byte[]{value});
        }
    }

    public String asciiValue(int address) {
        String string = new String(computer.getMemory(), address, 1, Charsets.US_ASCII) + "&nbsp;";
        return string;
    }

    public Iterable getDisplayRows() {
        return computer.getDisplay().getRows();
    }

    public Iterable getDisplayCols() {
        return computer.getDisplay().getRows();
    }

    public String colorForPixel(int row, int column) {
        short x = (short) column;
        short y = (short) row;
        short valueFor = computer.getDisplay().getValueFor(x, y);
        if (valueFor == 0) {
            return "#" + MTMCDisplay.DARK;
        } else if (valueFor == 1) {
            return "#" + MTMCDisplay.MEDIUM;
        } else if (valueFor == 2) {
            return "#" + MTMCDisplay.LIGHT;
        } else if (valueFor == 3) {
            return "#" + MTMCDisplay.WHITE;
        }
        throw new IllegalStateException("Bad display value: "  + valueFor);
    }

    public String getRegisterFormat() {
        return registerFormat.toString().toLowerCase();
    }

    public void toggleRegisterFormat() {
        registerFormat = DisplayFormat.values()[(registerFormat.ordinal() + 1) % 3];
    }

    public String getMemoryFormat() {
        return memoryFormat.toString().toLowerCase();
    }

    public void toggleMemoryFormat() {
        memoryFormat = DisplayFormat.values()[(memoryFormat.ordinal() + 1) % 3];
    }

    enum DisplayFormat {
        HEX,
        DEC,
        ASCII
    }
}
