package mtmc.web;

import mtmc.asm.instructions.Instruction;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.fs.Listing;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

public class MTMCWebView {

    public static final int COLS_FOR_MEM = 16;

    private final MonTanaMiniComputer computer;

    private DisplayFormat memoryDisplayFormat = DisplayFormat.DYN;

    private Set<String> expandedPaths = new HashSet<>();
    private String currentFileContent = null;

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
            DisplayFormat format = computeRegisterFormat(register);
            String str = displayValue(format, val, (short) 0);
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

    public String getVisualShell(){
        if(currentFileContent != null){
            return renderEditor();
        } else {
            return renderFileTree();
        }
    }

    private String renderEditor() {
        return "<div><button fx-swap='innerHTML' fx-target='#visual-shell' fx-action='/fs/close/'>close</button></div>" +
                "<textarea id='editor'>" + currentFileContent + "</textarea>";
    }

    @NotNull
    private String renderFileTree() {
        File rootFile = computer.getOS().loadFile("/");
        var sb = new StringBuilder();
        sb.append("<ul><li><code>/</code><ul>");
        var fs = computer.getFileSystem();
        // TODO: Nav through listing{listOfFiles = [], subdirectories = [] }

        // TODO: Implement appendListingContent(Listing listing){
        //  for (File file: listing.listOfFiles){
        //      appendContentForFile(file, sb);
        //  }
        //  }


//        for (File file : rootFile.listFiles()) {
//            appendContentForFile(file, sb);
//        }
        sb.append("</ul></li></ul>");
        return sb.toString();
    }

    private void appendContentForFile(File file, StringBuilder sb) {
        if (file.getName().startsWith(".")) {
            return;
        }
        if (file.isDirectory()) {
            appendDirectoryContent(file, sb);
        } else {
            appendFileContent(file, sb);
        }
    }

    private void appendFileContent(File file, StringBuilder sb) {
        sb.append("<li class='file-entry'>");
        sb.append("<a class='directory-entry' fx-swap='innerHTML' fx-target='#visual-shell' fx-action='/fs/open/")
                .append(file.getPath())
                .append("'>");
        sb.append(file.getName());
        sb.append("</a>");
        sb.append("</li>");
    }

    private void appendListingContent(Listing listing) {

        return;
    }

    private void appendDirectoryContent(File file, StringBuilder sb) {
        String path = file.getPath();
        sb.append("<li>");
        sb.append("<a class='directory-entry' fx-swap='innerHTML' fx-target='#visual-shell' fx-action='/fs/toggle/")
                .append(file.getPath())
                .append("'>");
        sb.append(file.getName());
        sb.append("</a>");
        if(expandedPaths.contains(path)) {
            sb.append("<ul>");
            for (File child : file.listFiles()) {
                appendContentForFile(child, sb);
            }
            sb.append("</ul>");
        }
        sb.append("</li>");
    }

    public String getMemoryTable(){
        StringBuilder builder = new StringBuilder("<table id='memory-table' style='width:100%; table-layout:fixed'>");
        byte[] memory = computer.getMemory();
        short previousValue = 0;
        for (int i = 0; i < memory.length; i++) {
            if (i % 16 == 0) {
                builder.append("<tr>");
            }

            // figure out how we are displaying this location
            String memoryClass = classFor(i);
            DisplayFormat format = computeMemoryFormat(memoryClass);
            int cols = computeCols(format);

            short val = (short) (memory[i] & 0xFF);
            int originalPos = i;
            if (cols == 2) {
                i++; // consuming a word for this cell
                val = (short) (val << 8);
                val = (short) (val | (memory[i] & 0xFF));
            }
            String displayStr = displayValue(format, val, previousValue);

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
            previousValue = val;
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

    private DisplayFormat computeMemoryFormat(String memoryClass) {
        if(memoryDisplayFormat == DisplayFormat.DYN) {
            return switch (memoryClass) {
                case "sta" -> DisplayFormat.DEC;
                case "code" -> DisplayFormat.INS;
                case "data", "heap" -> DisplayFormat.STR;
                default -> DisplayFormat.HEX;
            };
        } else {
            return memoryDisplayFormat;
        }
    }

    private DisplayFormat computeRegisterFormat(Register register) {
        return switch (register) {
            case IR -> DisplayFormat.INS;
            default -> DisplayFormat.DEC;
        };
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

    public String displayValue(DisplayFormat format, short val, short previousValue) {
        return switch (format) {
            case HEX -> String.format("%02X ", val);
            case DEC -> String.valueOf(val);
            case INS -> Instruction.disassemble(val, previousValue);
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


    public String getMemoryFormat() {
        return memoryDisplayFormat.name().toLowerCase();
    }

    public void toggleMemoryFormat() {
        int currentIndex = memoryDisplayFormat.ordinal() + 1;
        DisplayFormat[] vals = DisplayFormat.values();
        int nextIndex = (currentIndex + 1) % vals.length;
        memoryDisplayFormat = vals[nextIndex];
    }

    public void togglePath(String pathToToggle) {
        if(expandedPaths.contains(pathToToggle)) {
            expandedPaths.remove(pathToToggle);
        } else {
            expandedPaths.add(pathToToggle);
        }
    }

    public boolean openFile(String fileToOpen) {
        File file = computer.getOS().loadFile(fileToOpen);
        if(file.exists() && file.isFile()) {
            try {
                currentFileContent = Files.readString(file.toPath());
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
    }

    public void closeFile() {
        currentFileContent = null;
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
