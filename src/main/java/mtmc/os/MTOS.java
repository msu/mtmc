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
    Random random = new Random();

    public MTOS(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public void handleSysCall(short syscallNumber) {
        if (syscallNumber == SysCall.getValue("exit")) {
            computer.setStatus(MonTanaMiniComputer.ComputerStatus.FINISHED);
        } else if (syscallNumber == SysCall.getValue("rint")) {
            // rint
            short val = computer.getConsole().readInt();
            computer.setRegisterValue(RV, val);
        } else if (syscallNumber == SysCall.getValue("wint")) {
            // wint
            short value = computer.getRegisterValue(A0);
            computer.getConsole().writeInt(value);
        } else if (syscallNumber == SysCall.getValue("rstr")) {
            // rstr
            short pointer = computer.getRegisterValue(A0);
            short maxLen = computer.getRegisterValue(A1);
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
        } else if (syscallNumber == SysCall.getValue("rnd")) {
            // rnd
            short low = computer.getRegisterValue(A0);
            short high = computer.getRegisterValue(A1);
            computer.setRegisterValue(RV, random.nextInt(low, high + 1));
        } else if (syscallNumber == SysCall.getValue("sleep")) {
            // sleep
            short millis = computer.getRegisterValue(A0);
            try {
                Thread.sleep(millis);
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
        } else if (syscallNumber == SysCall.getValue("memcopy")) {
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
            File file = new File("disk/" + fileName);

            if (!file.exists()) {
                computer.setRegisterValue(RV, 0);
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
                            int offset = lineNum * 80 + colNum;
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
            } catch (IOException e) {
                computer.setRegisterValue(RV, -1);
            }

        }
    }

    @NotNull
    private String readStringFromMemory(short pointer) {
        short length = 0;
        while (computer.fetchByteFromMemory(pointer + length) != 0) {
            length++;
        }
        String outputString = new String(computer.getMemory(), pointer, length, Charsets.US_ASCII);
        return outputString;
    }

    public void processCommand(String command) {
        if (!command.trim().isEmpty()) {
            Shell.execCommand(command, computer);
        }
    }

    public File loadFile(String path) {
        File file = new File("disk/" + path);
        return file;
    }
}
