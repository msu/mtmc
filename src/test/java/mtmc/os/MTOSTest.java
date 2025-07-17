package mtmc.os;

import mtmc.emulator.MonTanaMiniComputer;
import kotlin.text.Charsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static mtmc.emulator.Register.*;

public class MTOSTest {


    @Test
    public void testReadInt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.getConsole().setShortValue((short) 10);
        short inst = 0b0000_0000_0000_0001; // sys rint
        computer.execInstruction(inst);
        assertEquals(10, computer.getRegisterValue(RV));
    }

    @Test
    public void testWriteInt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(A0, 10);
        short inst = 0b0000_0000_0000_0010; // sys wint
        computer.execInstruction(inst);
        assertEquals("10", computer.getConsole().getOutput());
    }

    @Test
    public void testReadStr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(A0, 10);
        computer.setRegisterValue(A1, 10);
        computer.getConsole().setStringValue("hello");
        short inst = 0b0000_0000_0000_0011; // sys rstr
        computer.execInstruction(inst);

        byte[] bytes = computer.getBytesFromMemory(10, 5);
        String str = new String(bytes, Charsets.US_ASCII);

        assertEquals("hello", str);
        assertEquals(5, computer.getRegisterValue(RV));
    }

    @Test
    public void testWriteStr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        byte[] bytes = "Hello world!".getBytes(Charsets.US_ASCII);
        computer.writeStringToMemory(50, bytes);
        computer.setRegisterValue(A0, 50);
        short inst = 0b0000_0000_0000_0110; // sys wstr
        computer.execInstruction(inst);
        assertEquals("Hello world!", computer.getConsole().getOutput());
    }

    @Test
    public void testAtoi() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        byte[] bytes = "42".getBytes(Charsets.US_ASCII);
        computer.writeStringToMemory(50, bytes);
        computer.setRegisterValue(A0, 50);
        short inst = 0x8; // atoi
        computer.execInstruction(inst);
        assertEquals(42, computer.getRegisterValue(RV));
    }

    @Test
    public void testAtoiWithSpace() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        byte[] bytes = "  42  ".getBytes(Charsets.US_ASCII);
        computer.writeStringToMemory(50, bytes);
        computer.setRegisterValue(A0, 50);
        short inst = 0x8; // atoi
        computer.execInstruction(inst);
        assertEquals(42, computer.getRegisterValue(RV));
    }

    @Test
    public void testAtoiWithTrailingContent() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        byte[] bytes = "  42  122 ".getBytes(Charsets.US_ASCII);
        computer.writeStringToMemory(50, bytes);
        computer.setRegisterValue(A0, 50);
        short inst = 0x8; // atoi
        computer.execInstruction(inst);
        assertEquals(42, computer.getRegisterValue(RV));
    }

    @Test
    public void testAtoiWithBadValueReturnsZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        byte[] bytes = "  asdf  122 ".getBytes(Charsets.US_ASCII);
        computer.writeStringToMemory(50, bytes);
        computer.setRegisterValue(A0, 50);
        short inst = 0x8; // atoi
        computer.execInstruction(inst);
        assertEquals(0, computer.getRegisterValue(RV));
    }

    @Test
    public void testRnd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(A0, 10);
        computer.setRegisterValue(A1, 20);
        short inst = 0b0000_0000_0010_0000; // sys rnd
        computer.execInstruction(inst);
        short returnVal = computer.getRegisterValue(RV);
        assertTrue(10 <= returnVal && returnVal <= 20);
    }

    @Test
    public void testSleep() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(A0, 100);
        long start = System.currentTimeMillis();
        short inst = 0b0000_0000_0010_0001; // sys sleep
        computer.execInstruction(inst);
        long end = System.currentTimeMillis();
        long duration = end - start;
        assertTrue(duration > 100, "Duration should be greater than 100 : " + duration);
    }

    @Test
    public void testTimer() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        short inst = 0b0000_0000_0010_0010; // sys timer
        
        computer.setRegisterValue(A0, 10);
        computer.execInstruction(inst);
        
        assertEquals(10, computer.getRegisterValue(RV));
        
        computer.setRegisterValue(A0, 0);
        computer.execInstruction(inst);
        
        // The time may tick by a ms by the time we run the check. So checking both 9 and 10
        assertTrue(computer.getRegisterValue(RV) == 10 || computer.getRegisterValue(RV) == 9);
        
        try { Thread.sleep(10); } catch(InterruptedException e) {}
        
        computer.execInstruction(inst);
        assertEquals(0, computer.getRegisterValue(RV)); // Timer exhausted
    }

}
