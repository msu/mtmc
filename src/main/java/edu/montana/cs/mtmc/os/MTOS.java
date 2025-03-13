package edu.montana.cs.mtmc.os;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import kotlin.text.Charsets;

import static edu.montana.cs.mtmc.emulator.Registers.*;

public class MTOS {

    private final MonTanaMiniComputer computer;

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
        }
    }
}
