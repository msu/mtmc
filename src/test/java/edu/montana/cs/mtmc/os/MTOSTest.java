package edu.montana.cs.mtmc.os;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import kotlin.text.Charsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static edu.montana.cs.mtmc.emulator.Registers.*;

public class MTOSTest {


    @Test
    public void testReadInt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.getConsole().setShortValue((short) 10);
        short inst = 0b0000_0000_0000_0001; // sys rint
        computer.execInstruction(inst);
        assertEquals(10, computer.getRegister(R0));
    }

    @Test
    public void testWriteInt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(A0, 10);
        short inst = 0b0000_0000_0000_0010; // sys wint
        computer.execInstruction(inst);
        assertEquals("10", computer.getConsole().getOutput());
    }

    @Test
    public void testReadStr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(A0, 10);
        computer.setRegister(A1, 10);
        computer.getConsole().setStringValue("hello");
        short inst = 0b0000_0000_0000_0011; // sys rstr
        computer.execInstruction(inst);

        byte[] bytes = computer.getBytes(10, 5);
        String str = new String(bytes, Charsets.US_ASCII);

        assertEquals("hello", str);
        assertEquals(5, computer.getRegister(R0));
    }

    @Test
    public void testWriteStr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        int address = 50;
        byte[] bytes = "Hello world!".getBytes(Charsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++) {
            computer.writeByte(address + i, bytes[i]);
        }
        computer.setRegister(A0, address);
        short inst = 0b0000_0000_0000_0100; // sys wstr
        computer.execInstruction(inst);
        assertEquals("Hello world!", computer.getConsole().getOutput());
    }

    @Test
    public void testRnd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(A0, 10);
        computer.setRegister(A1, 20);
        short inst = 0b0000_0000_0000_0111; // sys rnd
        computer.execInstruction(inst);
        short returnVal = computer.getRegister(R0);
        assertTrue(10 <= returnVal && returnVal <= 20);
    }

    @Test
    public void testSleep() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(A0, 100);
        long start = System.currentTimeMillis();
        short inst = 0b0000_0000_0000_1000; // sys sleep
        computer.execInstruction(inst);
        long end = System.currentTimeMillis();
        long duration = end - start;
        assertTrue(duration > 100, "Duration should be greater than 100 : " + duration);
    }

}
