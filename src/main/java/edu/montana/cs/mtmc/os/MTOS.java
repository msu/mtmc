package edu.montana.cs.mtmc.os;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.Shell;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;
import kotlin.text.Charsets;

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
            computer.setStatus(MonTanaMiniComputer.ComputerStatus.HALTED);
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
        if (!command.trim().isEmpty()) {
            Shell.execCommand(command, computer);
        }
    }

    public File loadFile(String path) {
        File file = new File("disk/" + path);
        return file;
    }
}
