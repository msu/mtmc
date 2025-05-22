package mtmc.web;

import mtmc.asm.instructions.Instruction;
import mtmc.emulator.MTMCDisplay;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.stream.IntStream;

public class MTMCWebView {

    public static final int COLS_FOR_MEM = 16;

    private final MonTanaMiniComputer computer;

    private DisplayFormat format = DisplayFormat.DYN;

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
            Register register = Register.valueOf(reg.toUpperCase());
            int regIndex = register.ordinal();
            short val = computer.getRegisterValue(regIndex);
            DisplayFormat format = computeFormat(register);
            String str = displayValue(format, val);
            return str;
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

    public String flagsBlinken() {
        StringBuilder blinken = new StringBuilder();
        blinken.append("t: <div class='blinken ");
        if (computer.isFlagTestBitSet()) {
            blinken.append("on");
        } else {
            blinken.append("off");
        }
        blinken.append("'></div>");
        blinken.append(" o: <div class='blinken ");
        blinken.append("off");
        blinken.append("'></div>");
        blinken.append(" e: <div class='blinken ");
        blinken.append("off");
        blinken.append("'></div>");
        return blinken.toString();
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


    public String getMemoryTable(){
        StringBuilder builder = new StringBuilder("<table id='memory-table' style='width:100%; table-layout:fixed'>");
        byte[] memory = computer.getMemory();
        for (int i = 0; i < memory.length; i++) {
            if (i % 16 == 0) {
                builder.append("<tr>");
            }

            // figure out how we are displaying this location
            String memoryClass = classFor(i);
            DisplayFormat format = computeFormat(memoryClass);
            int cols = computeCols(format);

            short val = (short) (memory[i] & 0xFF);
            int originalPos = i;
            if (cols == 2) {
                i++; // consuming a word for this cell
                val = (short) (val << 8);
                val = (short) (val | (memory[i] & 0xFF));
            }
            String displayStr = displayValue(format, val);

            // build table cell
            builder.append("<td title='");
            builder.append(originalPos);
            builder.append("' id='mem_");
            builder.append(originalPos);
            builder.append("'");
            builder.append("' class='");
            builder.append(memoryClass);
            builder.append("'");
            if (cols > 1) {
                builder.append(" colspan='");
                builder.append(cols);
                builder.append("'");
            }
            builder.append(">");
            builder.append(displayStr);
            builder.append("</td>");

            if (i % COLS_FOR_MEM == 15) {
                builder.append("</tr>");
            }
        }
        builder.append("</table>");
        return builder.toString();
    }

    private int computeCols(DisplayFormat format) {
        return switch (format) {
            case DEC, INS -> 2;
            default -> 1;
        };
    }

    private DisplayFormat computeFormat(String memoryClass) {
        if(format == DisplayFormat.DYN) {
            return switch (memoryClass) {
                case "sta" -> DisplayFormat.DEC;
                case "curr", "code" -> DisplayFormat.INS;
                case "data", "heap" -> DisplayFormat.STR;
                default -> DisplayFormat.HEX;
            };
        } else {
            return format;
        }
    }

    private DisplayFormat computeFormat(Register register) {
        if(format == DisplayFormat.DYN) {
            return switch (register) {
                case IR -> DisplayFormat.INS;
                default -> DisplayFormat.DEC;
            };
        } else {
            return format;
        }
    }

    public String classFor(int address) {
        if (address >= computer.getRegisterValue(Register.SP)) {
            return "sta";
        } else if (address == computer.getRegisterValue(Register.PC)) {
            return "curr";
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

    public String displayValue(DisplayFormat format, short val) {
        return switch (format) {
            case HEX -> String.format("%02X ", val);
            case DEC -> String.valueOf(val);
            case INS -> Instruction.disassembleInstruction(val);
            case STR -> get1252String(val);
            default -> throw new IllegalArgumentException("Can't render displayValue " + format);
        };
    }

    public static String get1252String(short value) {
        byte topByte = (byte) (value >>> 8);
        byte bottomByte = (byte) value;
        if (topByte != 0) {
            return get1252String(topByte) + " " + get1252String(bottomByte);
        } else {
            return get1252String(bottomByte);
        }
    }

    @NotNull
    private static String get1252String(byte value) {
        if (0 <= value && value < 32) {
            return ASCII_CODES[value];
        } else {
            Charset charset = Charset.forName("windows-1252");
            String str = new String(new byte[]{value}, charset);
            return str;
        }
    }

    static final String[] ASCII_CODES = new String[]{"NUL", "SOH", "STX", "ETX", "EOT", "ENT", "ACK", "BEL", "BS", "\\t",
            "\\n", "VT", "\\f", "\\r", "SO", "SI", "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB", "CAN", "EM",
            "SUB", "ESC", "FS", "GS", "RS", "US",
    };


    public String classForPixel(int row, int column) {
        short x = (short) column;
        short y = (short) row;
        short valueFor = computer.getDisplay().getPixel(x, y);
        if (valueFor == 0) {
            return "d";
        } else if (valueFor == 1) {
            return "m";
        } else if (valueFor == 2) {
            return "l";
        } else if (valueFor == 3) {
            return "w";
        }
        throw new IllegalStateException("Bad display value: "  + valueFor);
    }



    public void toggleFormat() {
        format = DisplayFormat.values()[(format.ordinal() + 1) % format.values().length];
    }


    enum DisplayFormat {
        DYN,
        HEX,
        DEC,
        INS,
        STR
    }

    public static void main(String[] args) {
        for (int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            System.out.println(i + " : " + get1252String((short) i));
        }
    }
}
