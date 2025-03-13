package edu.montana.cs.mtmc.emulator;

import org.junit.jupiter.api.Test;

import static edu.montana.cs.mtmc.emulator.MonTanaMiniComputer.WORD_SIZE;
import static edu.montana.cs.mtmc.emulator.Registers.*;
import static org.junit.jupiter.api.Assertions.*;

public class MonTanaMiniComputerTest {

    @Test
    void testMove() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0);
        computer.setRegister(T1, 10);

        short moveInst = 0b0000_0001_0000_0001; // mv t0, t1
        computer.execInstruction(moveInst);

        assertEquals(10, computer.getRegister(T0));
        assertEquals(10, computer.getRegister(T1));
    }

    @Test
    void testAdd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 5);
        computer.setRegister(T1, 10);

        short addInst = 0b0001_0000_0000_0001; // add t0, t1
        computer.execInstruction(addInst);

        assertEquals(15, computer.getRegister(T0));
        assertEquals(10, computer.getRegister(T1));
    }

    @Test
    void testPush() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 5);

        short pushInst = 0b0010_0000_0000_1011; // push t0, sp
        computer.execInstruction(pushInst);

        int newStackAddress = MonTanaMiniComputer.FRAME_BUFF_START - WORD_SIZE;
        assertEquals(newStackAddress, computer.getRegister(SP));
        assertEquals(5, computer.fetchWord(newStackAddress));
    }

    @Test
    void testLoadImmediate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();

        short loadImmediateInst = (short) 0b1000_0000_0000_0101; // ldi t0, 5
        computer.execInstruction(loadImmediateInst);

        assertEquals(5, computer.getRegister(T0));
    }

    @Test
    void testCall() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(PC, 50);

        short callInst = (short) 0b0011_0000_0001_0000; // call 16
        computer.execInstruction(callInst);

        assertEquals(16, computer.getRegister(PC));
        assertEquals(50 + WORD_SIZE, computer.getRegister(RA));
    }

    @Test
    void testJump() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(PC, 50);

        short jumpInst = (short) 0b0100_0000_0001_0000; // j 16
        computer.execInstruction(jumpInst);

        assertEquals(16, computer.getRegister(PC));
    }

    @Test
    void testJumpIfZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(PC, 50);

        short jumpIfZeroInst = (short) 0b0101_0000_0001_0000; // jz 16
        computer.execInstruction(jumpIfZeroInst);

        assertEquals(50, computer.getRegister(PC));

        computer.setRegister(T0, 0);
        computer.execInstruction(jumpIfZeroInst);
        assertEquals(16, computer.getRegister(PC));

    }

    @Test
    void testJumpIfNotZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0);
        computer.setRegister(PC, 50);

        short jumpIfNotZeroInst = (short) 0b0110_0000_0001_0000; // jnz 16
        computer.execInstruction(jumpIfNotZeroInst);

        assertEquals(50, computer.getRegister(PC));

        computer.setRegister(T0, 1);
        computer.execInstruction(jumpIfNotZeroInst);
        assertEquals(16, computer.getRegister(PC));

    }

    @Test
    void testJumpIfGreaterThanZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0);
        computer.setRegister(PC, 50);

        short jumpIfGreaterThanZero = (short) 0b0111_0000_0001_0000; // jgz 16
        computer.execInstruction(jumpIfGreaterThanZero);

        assertEquals(50, computer.getRegister(PC));

        computer.setRegister(T0, 5);
        computer.execInstruction(jumpIfGreaterThanZero);
        assertEquals(16, computer.getRegister(PC));
    }

    @Test
    void testLoadWord() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 0);
        computer.writeWord(100, (short) 10); // value written to memory

        short loadWordInst = (short) 0b1100_0000_0001_0010; // lw t0, t1, t2
        computer.execInstruction(loadWordInst);

        assertEquals(10, computer.getRegister(T0)); // should be loaded into t0
    }

    @Test
    void testLoadWordWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 5);
        computer.writeWord(105, (short) 10); // value written to memory

        short loadWordInst = (short) 0b1100_0000_0001_0010; // lw t0, t1, t2
        computer.execInstruction(loadWordInst);

        assertEquals(10, computer.getRegister(T0)); // should be loaded into t0
    }

    @Test
    void testLoadByte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 0);
        computer.writeByte(100, (byte) 10); // value written to memory

        short loadByteInst = (short) 0b1101_0000_0001_0010; // lb t0, t1, t2
        computer.execInstruction(loadByteInst);

        assertEquals(10, computer.getRegister(T0)); // should be loaded into t0
    }

    @Test
    void testLoadByteWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 5);
        computer.writeByte(105, (byte) 10); // value written to memory

        short loadByteInst = (short) 0b1101_0000_0001_0010; // lb t0, t1, t2
        computer.execInstruction(loadByteInst);

        assertEquals(10, computer.getRegister(T0)); // should be loaded into t0
    }

    @Test
    void testSaveWord() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 0);

        short saveWordInst = (short) 0b1110_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveWordInst);

        assertEquals(10, computer.fetchWord(100));
    }

    @Test
    void testSaveWordWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 5);

        short saveWordInst = (short) 0b1110_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveWordInst);

        assertEquals(10, computer.fetchWord(105));
    }

    @Test
    void testSaveByte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 0);

        short saveByteInst = (short) 0b1111_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveByteInst);

        assertEquals(10, computer.fetchByte(100));
    }

    @Test
    void testSaveByteWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 100);
        computer.setRegister(T2, 5);

        short saveByteInst = (short) 0b1111_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveByteInst);

        assertEquals(10, computer.fetchByte(105));
    }

}
