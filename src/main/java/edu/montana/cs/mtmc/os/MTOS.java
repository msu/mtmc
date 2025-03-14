package edu.montana.cs.mtmc.os;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.utils.ImageUtils;
import edu.montana.cs.mtmc.web.WebServer;
import kotlin.text.Charsets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static edu.montana.cs.mtmc.emulator.Registers.*;

public class MTOS {

    private final MonTanaMiniComputer computer;
    Random random = new Random();

    public MTOS(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public void handleSysCall(short syscallNumber) {
        if (syscallNumber == 0x0000) {
            // TODO end currently executing program
        } else if (syscallNumber == 0x0001) {
            // rint
            short val = computer.getConsole().readInt();
            computer.setRegister(R0, val);
        } else if (syscallNumber == 0x0002) {
            // wint
            short value = computer.getRegister(A0);
            computer.getConsole().writeInt(value);
        } else if (syscallNumber == 0x0003) {
            // rstr
            short pointer = computer.getRegister(A0);
            short maxLen = computer.getRegister(A1);
            String string = computer.getConsole().readString();
            byte[] bytes = string.getBytes(Charsets.US_ASCII);
            int bytesToRead = Math.min(bytes.length, maxLen);
            for (int i = 0; i < bytesToRead; i++) {
                byte aByte = bytes[i];
                computer.writeByte(pointer + i, aByte);
            }
            computer.setRegister(R0, bytesToRead);
        } else if (syscallNumber == 0x0004) {
            // wstr
            short pointer = computer.getRegister(A0);
            short length = 0;
            while (computer.fetchByte(pointer + length) != 0) {
                length++;
            }
            String outputString = new String(computer.getMemory(), pointer, length, Charsets.US_ASCII);
            computer.getConsole().print(outputString);
        } else if (syscallNumber == 0x0007) {
            // rnd
            short low = computer.getRegister(A0);
            short high = computer.getRegister(A1);
            computer.setRegister(R0, random.nextInt(low, high + 1));
        } else if (syscallNumber == 0x0008) {
            // sleep
            short millis = computer.getRegister(A0);
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if (syscallNumber == 0x0009) {
            // fbreset
            byte[] memory = computer.getMemory();
            for (int i = MonTanaMiniComputer.FRAME_BUFF_START; i < memory.length; i++) {
                memory[i] = 0;
            }
        } else if (syscallNumber == 0x000A) {
            // fbstat
            short x = computer.getRegister(A0);
            short y = computer.getRegister(A1);
            short val = computer.getDisplay().getValueFor(x, y);
            computer.setRegister(R0, val);
        } else if (syscallNumber == 0x000A) {
            // fbset
            short x = computer.getRegister(A0);
            short y = computer.getRegister(A1);
            short color = computer.getRegister(A3);
            computer.getDisplay().setValueFor(x, y, color);
        }
    }

    public void processCommand(String command) {
        try {
            if (!command.isEmpty()) {
                if (command.equals("q")) {
                    System.exit(1);
                } else if(command.equals("web")) {
                    WebServer server = WebServer.getInstance(computer);
                    Desktop.getDesktop().browse(server.getURL());
                } else if(command.startsWith("display ")) {
                    String[] commandSplit = command.split(" ");
                    boolean printUsage = true;
                    if (commandSplit.length == 2) {
                        command = commandSplit[1];
                        if (command.equals("fuzz")) {
                            Iterable<Integer> rows = computer.getDisplay().getRows();
                            Iterable<Integer> cols = computer.getDisplay().getColumns();
                            for (Integer row : rows) {
                                for (Integer col : cols) {
                                    computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), (short) random.nextInt(0, 4));
                                }
                            }
                            printUsage = false;
                        } else if (command.equals("reset")) {
                            Iterable<Integer> rows = computer.getDisplay().getRows();
                            Iterable<Integer> cols = computer.getDisplay().getColumns();
                            for (Integer row : rows) {
                                for (Integer col : cols) {
                                    computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), (short) 0);
                                }
                            }
                            printUsage = false;
                        } else if (command.equals("invert")) {
                            Iterable<Integer> rows = computer.getDisplay().getRows();
                            Iterable<Integer> cols = computer.getDisplay().getColumns();
                            for (Integer row : rows) {
                                for (Integer col : cols) {
                                    short inverted = (short) (3 - computer.getDisplay().getValueFor(row.shortValue(), col.shortValue()));
                                    computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), inverted);
                                }
                            }
                            printUsage = false;
                        }
                    } else if (commandSplit.length == 3) {
                        command = commandSplit[1];
                        if (command.equals("image")) {
                            String imagePath = commandSplit[2];
                            File file = new File("disk/" + imagePath);
                            BufferedImage img = ImageIO.read(file);
                            Dimension scaleDimensions = ImageUtils.getScaledDimension(img, 64, 64);
                            BufferedImage scaledImage = ImageUtils.scaleImage(img, scaleDimensions);
                            int xpad = (64 - scaledImage.getWidth()) / 2;
                            int ypad = (64 - scaledImage.getHeight()) / 2;
                            for (int x = 0; x < scaledImage.getWidth(); x++) {
                                for (int y = 0; y < scaledImage.getHeight(); y++) {
                                    int twoBitValue = ImageUtils.findClosestDisplayColor(scaledImage.getRGB(x, y));
                                    computer.getDisplay().setValueFor((short) (x + xpad), (short) (y + ypad), (short) twoBitValue);
                                }
                            }
                            printUsage = false;
                        }
                    } else if (commandSplit.length == 4) {
                        Integer row = Integer.valueOf(commandSplit[1]);
                        Integer col = Integer.valueOf(commandSplit[2]);
                        Integer color = Integer.valueOf(commandSplit[3]);
                        computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), color.shortValue());
                        printUsage = false;
                    }
                    if(printUsage) {
                        computer.getConsole().println("display command options: fuzz, reset, image <file>, <row> <col> <color>");
                    }
                } else {
                    computer.getConsole().println("Unknown command: " + command);
                }
            }
        } catch (Exception e) {
            computer.getConsole().println("Error: " + e.getMessage());
        }

    }

}
