package mtmc.web;

import mtmc.asm.instructions.Instruction;
import mtmc.emulator.MTMCDisplay;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.IntStream;

public class MTMCWebView {

    private final MonTanaMiniComputer computer;

    private DisplayFormat registerFormat = DisplayFormat.DYN;
    private DisplayFormat memoryFormat = DisplayFormat.DYN;

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
        short value = computer.getRegisterValue(register);
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
                case DYN -> {
                    if (Objects.equals(reg, "IR")) {
                        return Instruction.disassembleInstruction(value);
                    } else {
                        return getHexStr(value);
                    }
                }
                case HEX -> {
                    return getHexStr(value);
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

    @NotNull
    private static String getHexStr(short value) {
        String str = Integer.toHexString(value & 0xffff);
        String padded = "%1$4s".formatted(str).replace(" ", "0");
        return "0x" + padded;
    }

    public String regBlinken(String reg) {
        Integer regIndex = Register.toInteger(reg);
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
        Integer index = Register.toInteger(reg);
        short register = computer.getRegisterValue(index);
        return register;
    }


    public String classFor(int address) {
        if (address >= MonTanaMiniComputer.FRAME_BUFF_START) {
            return "frameBuffer";
        } else if (address >= computer.getRegisterValue(Register.SP)) {
            return "stack";
        } else if (address == computer.getRegisterValue(Register.PC)) {
            return "currentInst";
        } else if (address <= computer.getRegisterValue(Register.CB)) {
            return "code";
        } else if (address <= computer.getRegisterValue(Register.DB)) {
            return "data";
        } else if (address <= computer.getRegisterValue(Register.BP)) {
            return "heap";
        } else {
            return "";
        }
    }

    public String displayValue(int address) {
        // TODO suport dyn view mode
        if (memoryFormat == DisplayFormat.HEX || memoryFormat == DisplayFormat.DYN) {
            byte byteVal = computer.fetchByteFromMemory(address);
            return String.format("%02X ", byteVal);
        } else if (memoryFormat == DisplayFormat.DEC) {
            if (address % 2 == 1) {
                return "" + computer.fetchWordFromMemory(address - 1);
            } else {
                return "";
            }
        } else {
            byte value = computer.fetchByteFromMemory(address);
            return new String(new byte[]{value});
        }
    }

    public String asciiValue(int address) {
        String string = new String(computer.getBytesFromMemory(address, 1), Charsets.US_ASCII) + "&nbsp;";
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
        DYN,
        HEX,
        DEC,
        ASCII
    }
}
