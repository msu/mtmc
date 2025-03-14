package edu.montana.cs.mtmc.web;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.emulator.Registers;
import kotlin.text.Charsets;

import java.lang.reflect.Field;

public class MTMCWebView {

    private final MonTanaMiniComputer computer;

    public MTMCWebView(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public Iterable getMemoryAddresses(){
        return computer.getMemoryAddresses();
    }

    public String regHex(String reg) {
        try {
            short value = getRegisterValue(reg);
            String hexString = Integer.toHexString(value);
            String paddedHexString = "%1$4s".formatted(hexString).replaceAll(" ", "0");
            return "0x" + paddedHexString;
        } catch (Exception e) {
            return "No such register: " + reg;
        }
    }
    public String regBin(String reg) {
        try {
            short value = getRegisterValue(reg);
            String binaryString = Integer.toBinaryString(value);
            String string = "%1$16s".formatted(binaryString).replaceAll(" ", "0");
            string = string.replaceAll("....", "$0 ");
            return string;
        } catch (Exception e) {
            return "No such register: " + reg;
        }
    }

    private short getRegisterValue(String reg) throws NoSuchFieldException, IllegalAccessException {
        Field field = Registers.class.getField(reg);
        Integer index = (Integer) field.get(null);
        short register = computer.getRegister(index);
        return register;
    }

    public String classFor(int address) {
        if (address >= MonTanaMiniComputer.FRAME_BUFF_START) {
            return "frameBuffer";
        } else if (address > computer.getRegister(Registers.SP)) {
            return "stack";
        } else {
            // TODO - color stack pointer, frame pointer, etc. cells, color text, data and heap segs
            return "";
        }
    }

    public String hexValue(int address) {
        return computer.getHex(address);
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
        short valueFor = computer.getDisplay().getValueFor((short) row, (short) column);
        if (valueFor == 0) {
            return "#2a453b";
        } else if (valueFor == 1) {
            return "#365d48";
        } else if (valueFor == 2) {
            return "#577c44";
        } else if (valueFor == 3) {
            return "#7f860f";
        }
        throw new IllegalStateException("Bad display value: "  + valueFor);
    }

}
