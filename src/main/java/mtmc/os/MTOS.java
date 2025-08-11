package mtmc.os;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.Shell;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static mtmc.emulator.Register.*;

public class MTOS {

    private final MonTanaMiniComputer computer;
    private long timer = 0;
    Random random = new Random();
    
    // Editor support
    private String currentFile;
    private String currentFileMime;
    private int[] breakpoints;

    public MTOS(MonTanaMiniComputer computer) {
        this.computer = computer;
    }    

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public String getCurrentFileMime() {
        return currentFileMime;
    }

    public void setCurrentFileMime(String currentFileMime) {
        this.currentFileMime = currentFileMime;
    }

    public int[] getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(int[] breakpoints) {
        this.breakpoints = breakpoints;
    }
    
    public void applyBreakpoints() {
        if (computer.getDebugInfo() == null || breakpoints == null) {
            return;
        }
        
        var debug = computer.getDebugInfo().originalLineNumbers();
        var name = computer.getDebugInfo().originalFile();
        
        if (getCurrentFileMime().equals("text/x-asm")) {
            debug = computer.getDebugInfo().assemblyLineNumbers();
            name = computer.getDebugInfo().assemblyFile();
        } 

        if (debug == null || !name.equals(currentFile)) {
            return;
        }

        for (int index=0; index<breakpoints.length; index++) {
            var line = breakpoints[index];
            for (int i=0; i<debug.length; i++) {
                if(debug[i] == line) {
                    computer.setBreakpoint(i, true);
                    break;
                }
            }
        }
    }

    public void handleSysCall(short syscallNumber) {
        if (syscallNumber == SysCall.getValue("exit")) {
            computer.setStatus(MonTanaMiniComputer.ComputerStatus.FINISHED);
        } else if (syscallNumber == SysCall.getValue("rint")) {
            // rint
            if(!computer.getConsole().hasShortValue()) {
                computer.notifyOfRequestInteger();
            }
            while(!computer.getConsole().hasShortValue() && computer.getStatus() == MonTanaMiniComputer.ComputerStatus.EXECUTING) {
                try { Thread.sleep(10); } catch(InterruptedException e) {}
            }
            short val = computer.getConsole().readInt();
            computer.setRegisterValue(RV, val);
        } else if (syscallNumber == SysCall.getValue("wint")) {
            // wint
            short value = computer.getRegisterValue(A0);
            computer.getConsole().writeInt(value);
        } else if (syscallNumber == SysCall.getValue("rchr")) {
            if(!computer.getConsole().hasShortValue()) {
                computer.notifyOfRequestCharacter();
            }
            while(!computer.getConsole().hasShortValue() && computer.getStatus() == MonTanaMiniComputer.ComputerStatus.EXECUTING) {
                try { Thread.sleep(10); } catch(InterruptedException e) {}
            }
            char val = computer.getConsole().readChar();
            computer.setRegisterValue(RV, val);
        } else if (syscallNumber == SysCall.getValue("wchr")) {
            short value = computer.getRegisterValue(A0);
            computer.getConsole().print("" + (char) value);
        } else if (syscallNumber == SysCall.getValue("rstr")) {
            // rstr
            short pointer = computer.getRegisterValue(A0);
            short maxLen = computer.getRegisterValue(A1);
            if(!computer.getConsole().hasReadString()) {
                computer.notifyOfRequestString();
            }
            while(!computer.getConsole().hasReadString() && computer.getStatus() == MonTanaMiniComputer.ComputerStatus.EXECUTING) {
                try { Thread.sleep(10); } catch(InterruptedException e) {}
            }
            String string = computer.getConsole().readString();
            byte[] bytes = string.getBytes(Charsets.US_ASCII);
            int bytesToRead = Math.min(bytes.length, maxLen);
            for (int i = 0; i < bytesToRead; i++) {
                byte aByte = bytes[i];
                computer.writeByteToMemory(pointer + i, aByte);
            }
            computer.setRegisterValue(RV, bytesToRead);
        } else if (syscallNumber == SysCall.getValue("wstr")) {
            // wstr
            short pointer = computer.getRegisterValue(A0);
            String outputString = readStringFromMemory(pointer);
            computer.getConsole().print(outputString);
        } else if (syscallNumber == SysCall.getValue("printf")) {
            short pointer = computer.getRegisterValue(A0);
            short initSP = computer.getRegisterValue(A1);
            String fmtString = readStringFromMemory(pointer);
            StringBuilder sb = new StringBuilder();
            int stackOff = 0;
            int i = 0;
            while (i < fmtString.length()) {
                char c = fmtString.charAt(i++);
                if (c != '%') {
                    sb.append(c);
                    continue;
                }

                if (i >= fmtString.length()) break;
                c = fmtString.charAt(i++);

                if (c == 'd') {
                    stackOff += 2;
                    int v = computer.fetchWordFromMemory(initSP - stackOff);
                    sb.append(v);
                } else if (c == 'c') {
                    stackOff += 2;
                    char v = (char) computer.fetchWordFromMemory(initSP - stackOff);
                    sb.append(v);
                } else if (c == 's') {
                    stackOff += 2;
                    short valuePointer = computer.fetchWordFromMemory(initSP - stackOff);
                    String s = readStringFromMemory(valuePointer);
                    sb.append(s);
                } else {
                    sb.append('%').append(c);
                }
            }

            computer.getConsole().print(sb.toString());
            computer.setRegisterValue(RV, sb.length());
        } else if (syscallNumber == SysCall.getValue("atoi")) {
            short pointer = computer.getRegisterValue(A0);
            String string = readStringFromMemory(pointer);
            String[] split = string.trim().split("\\s+");
            String firstNum = split[0];
            try {
                short value = Short.parseShort(firstNum);
                computer.setRegisterValue(RV, value);
            } catch(NumberFormatException e) {
                computer.setRegisterValue(RV, 0);
            }
        } else if (syscallNumber == SysCall.getValue("rnd")) {
            // rnd
            short low = computer.getRegisterValue(A0);
            short high = computer.getRegisterValue(A1);
            short temp;
            
            if(low > high) {
                temp = low;
                low = high;
                high = temp;
            }

            computer.setRegisterValue(RV, random.nextInt(low, high + 1));
        } else if (syscallNumber == SysCall.getValue("sleep")) {
            // sleep
            short millis = computer.getRegisterValue(A0);
            try {
                if(millis > 0) Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if (syscallNumber == SysCall.getValue("fbreset")) {
            // fbreset
            computer.getDisplay().reset();
        } else if (syscallNumber == SysCall.getValue("fbstat")) {
            // fbstat
            short x = computer.getRegisterValue(A0);
            short y = computer.getRegisterValue(A1);
            short val = computer.getDisplay().getPixel(x, y);
            computer.setRegisterValue(RV, val);
        } else if (syscallNumber == SysCall.getValue("fbset")) {
            // fbset
            short x = computer.getRegisterValue(A0);
            short y = computer.getRegisterValue(A1);
            short color = computer.getRegisterValue(A2);
            computer.getDisplay().setPixel(x, y, color);
        } else if (syscallNumber == SysCall.getValue("fbline")) {
            short startX = computer.getRegisterValue(A0);
            short startY = computer.getRegisterValue(A1);
            short endX = computer.getRegisterValue(A2);
            short endY = computer.getRegisterValue(A3);
            computer.getDisplay().drawLine(startX, startY, endX, endY);
        } else if (syscallNumber == SysCall.getValue("fbrect")) {
            short startX = computer.getRegisterValue(A0);
            short startY = computer.getRegisterValue(A1);
            short width = computer.getRegisterValue(A2);
            short height = computer.getRegisterValue(A3);
            computer.getDisplay().drawRectangle(startX, startY, width, height);
        } else if (syscallNumber == SysCall.getValue("fbflush")) {
            computer.getDisplay().sync();
        } else if (syscallNumber == SysCall.getValue("joystick")) {
            computer.setRegisterValue(RV, computer.getIOState());
        } else if (syscallNumber == SysCall.getValue("scolor")) {
            computer.getDisplay().setColor(computer.getRegisterValue(A0));
        } else if (syscallNumber == SysCall.getValue("memcpy")) {
            short fromPointer = computer.getRegisterValue(A0);
            short toPointer = computer.getRegisterValue(A1);
            short bytes = computer.getRegisterValue(A2);
            for (int i = 0; i < bytes; i++) {
                byte b = computer.fetchByteFromMemory(fromPointer + i);
                computer.writeByteToMemory(toPointer + i, b);
            }
        } else if (syscallNumber == SysCall.getValue("rfile")) {
            short fileNamePtr = computer.getRegisterValue(A0);
            String fileName = readStringFromMemory(fileNamePtr);
            File file = new File("disk" + computer.getFileSystem().resolve(fileName));

            if (!file.exists()) {
                computer.setRegisterValue(RV, 1);
                return;
            }

            short destination = computer.getRegisterValue(A1);

            short maxSize1 = computer.getRegisterValue(A2);
            short maxSize2 = computer.getRegisterValue(A3);

            String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);

            try {
                // special handling for game-of-life cell files
                if ("cells".equals(fileType)) {

                    String str = Files.readString(file.toPath());
                    List<String> lines = Arrays
                            .stream(str.split("\n"))
                            .filter(s -> !s.startsWith("!"))
                            .toList();

                    int linesTotal = lines.size();
                    int cappedLines = Math.min(linesTotal, maxSize2);

                    for (int lineNum = 0; lineNum < cappedLines; lineNum++) {
                        String line = lines.get(lineNum);
                        for (int colNum = 0; colNum < maxSize1; colNum++) {
                            int offset = lineNum * maxSize1 + colNum;
                            int byteOffset = offset / 8;
                            int bitOffset = offset % 8;
                            byte currentVal = computer.fetchByteFromMemory(destination + byteOffset);
                            int mask = 1 << bitOffset;
                            byte newVal;
                            if (colNum < line.length() && line.charAt(colNum) == 'O') {
                                newVal = (byte) (currentVal | mask);
                            } else {
                                newVal = (byte) (currentVal & ~mask);
                            }
                            computer.writeByteToMemory(destination + byteOffset, newVal);
                        }
                    }
                } else {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    for (int i = 0; i < maxSize1; i++) {
                        byte aByte = bytes[i];
                        computer.writeByteToMemory(destination + i, aByte);
                    }
                }
                computer.setRegisterValue(RV, 0);
            } catch (IOException e) {
                computer.setRegisterValue(RV, -1);
                e.printStackTrace(); // debugging
            }

        } else if (syscallNumber == SysCall.getValue("cwd")) {
        
            String cwd = computer.getFileSystem().listCWD().path;

            short destination = computer.getRegisterValue(A0);
            int maxSize = Math.min(computer.getRegisterValue(A1), cwd.length()+1);

            for (int i = 0; i < maxSize-1; i++) {
                byte aByte = (byte)cwd.charAt(i);
                computer.writeByteToMemory(destination + i, aByte);
            }
            
            //TODO: Should this return the length with or without the null terminator?
            computer.writeByteToMemory(destination + maxSize - 1, (byte)0);
            computer.setRegisterValue(RV, maxSize-1);
        } else if (syscallNumber == SysCall.getValue("chdir")) {

            short pointer = computer.getRegisterValue(A0);
            String dir = readStringFromMemory(pointer);
            
            if (computer.getFileSystem().exists(dir)) {
                computer.setRegisterValue(RV, 0);
                computer.getFileSystem().setCWD(dir);
            } else {
                computer.setRegisterValue(RV, 1);
            }
        } else if (syscallNumber == SysCall.getValue("timer")) {
            short value = computer.getRegisterValue(A0);
            
            if (value > 0) this.timer = System.currentTimeMillis() + value;
            
            computer.setRegisterValue(RV, (int)Math.max(0, this.timer - System.currentTimeMillis()));
        } else if (syscallNumber == SysCall.getValue("drawimg")) {
            short image = computer.getRegisterValue(A0);
            short x = computer.getRegisterValue(A1);
            short y = computer.getRegisterValue(A2);
            
            if (!computer.getDisplay().hasGraphic(image)) {
                computer.setRegisterValue(RV, 1);
                return;
            }
            
            computer.getDisplay().drawImage(image, x, y);
            computer.setRegisterValue(RV, 0);
        } else if (syscallNumber == SysCall.getValue("drawimgsz")) {
            short image = computer.getRegisterValue(A0);
            short pointer = computer.getRegisterValue(A1);
            short x = computer.fetchWordFromMemory(pointer);
            short y = computer.fetchWordFromMemory(pointer + 2);
            short width = computer.fetchWordFromMemory(pointer + 4);
            short height = computer.fetchWordFromMemory(pointer + 6);
            
            if (!computer.getDisplay().hasGraphic(image)) {
                computer.setRegisterValue(RV, 1);
                return;
            }
            
            computer.getDisplay().drawImage(image, x, y, width, height);
            computer.setRegisterValue(RV, 0);
        } else if (syscallNumber == SysCall.getValue("drawimgclip")) {
            short image = computer.getRegisterValue(A0);
            short source = computer.getRegisterValue(A1);
            short destination = computer.getRegisterValue(A2);
            
            short sx = computer.fetchWordFromMemory(source);
            short sy = computer.fetchWordFromMemory(source + 2);
            short sw = computer.fetchWordFromMemory(source + 4);
            short sh = computer.fetchWordFromMemory(source + 6);
            
            short dx = computer.fetchWordFromMemory(destination);
            short dy = computer.fetchWordFromMemory(destination + 2);
            short dw = computer.fetchWordFromMemory(destination + 4);
            short dh = computer.fetchWordFromMemory(destination + 6);
            
            if (!computer.getDisplay().hasGraphic(image)) {
                computer.setRegisterValue(RV, 1);
                return;
            }
            
            computer.getDisplay().drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh);
            computer.setRegisterValue(RV, 0);
        } else if (syscallNumber == SysCall.getValue("dirent")) {
            short dirent = computer.getRegisterValue(A0);
            short command = computer.getRegisterValue(A1);
            short offset = computer.getRegisterValue(A2);
            short destination = computer.getRegisterValue(A3);
            
            short maxSize = computer.fetchWordFromMemory(dirent + 2);
            short maxSizeOut = computer.fetchWordFromMemory(destination + 2);
            
            String dir = readStringFromMemory(dirent);
            File[] list;

            if (!computer.getFileSystem().exists(dir)) {
                computer.setRegisterValue(RV, -1);
                return;
            }
            
            list = computer.getFileSystem().getFileList(dir);
            
            if (command == 0) { // Count of files in the directory
                computer.setRegisterValue(RV, list.length);
            } if (command == 1) {

                if (offset < 0 || offset >= list.length) {
                    computer.setRegisterValue(RV, -2);
                    return;
                }
                
                File file = list[offset];
                String name = file.getName();
                int size = Math.min(maxSizeOut-1, name.length());

                computer.writeWordToMemory(destination, file.isDirectory() ? 1 : 0);
                
                for (int i = 0; i < size; i++) {
                    byte aByte = (byte)name.charAt(i);
                    computer.writeByteToMemory(destination + 4 + i, aByte);
                }

                //TODO: Should this return the length with or without the null terminator?
                computer.writeByteToMemory(destination + 4 + size, (byte)0);
                computer.setRegisterValue(RV, Math.min(maxSizeOut, name.length())-1);
            }
        } else if (syscallNumber == SysCall.getValue("dfile")) {
            short pointer = computer.getRegisterValue(A0);
            String path = readStringFromMemory(pointer);
            
            if (computer.getFileSystem().delete(path)) {
                computer.setRegisterValue(RV, 0);
            } else {
                computer.setRegisterValue(RV, 1);
            }
        }
    }

    @NotNull
    private String readStringFromMemory(short pointer) {
        short length = 0;
        while (computer.fetchByteFromMemory(pointer + length) != 0) {
            length++;
        }
        try {
            String outputString = new String(computer.getMemory(), pointer, length, Charsets.US_ASCII);
            return outputString;
        } catch (StringIndexOutOfBoundsException ignored) {
            computer.setStatus(MonTanaMiniComputer.ComputerStatus.PERMANENT_ERROR);
            return "";
        }
    }

    public void processCommand(String command) {
        if (!command.trim().isEmpty()) {
            Shell.execCommand(command, computer);
        }
    }

    public File loadFile(String path) {
        var fs = computer.getFileSystem();
        File file = fs.getRealPath(path).toFile();
        return file;
    }
}
