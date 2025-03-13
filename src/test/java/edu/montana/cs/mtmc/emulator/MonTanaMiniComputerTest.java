package edu.montana.cs.mtmc.emulator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
    void testNoOp() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();

        short[] originalRegisters = Arrays.copyOf(computer.registerFile, computer.registerFile.length);
        byte[] originalMemory = Arrays.copyOf(computer.memory, computer.memory.length);

        short noop = 0b0000_1111_1111_1111; // no-op
        computer.execInstruction(noop);

        boolean registersEqual = Arrays.equals(originalRegisters, computer.registerFile);
        assertTrue(registersEqual);
        boolean memoryEqual = Arrays.equals(originalMemory, computer.memory);
        assertTrue(memoryEqual);
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
    void testSub() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 5);

        short subInst = 0b0001_0001_0000_0001; // sub t0, t1
        computer.execInstruction(subInst);

        assertEquals(5, computer.getRegister(T0));
        assertEquals(5, computer.getRegister(T1));
    }

    @Test
    void testMul() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 5);

        short mulInst = 0b0001_0010_0000_0001; // mul t0, t1
        computer.execInstruction(mulInst);

        assertEquals(50, computer.getRegister(T0));
        assertEquals(5, computer.getRegister(T1));
    }

    @Test
    void testDiv() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 5);

        short divInst = 0b0001_0011_0000_0001; // div t0, t1
        computer.execInstruction(divInst);

        assertEquals(2, computer.getRegister(T0));
        assertEquals(5, computer.getRegister(T1));
    }

    @Test
    void testMod() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);
        computer.setRegister(T1, 5);

        short modInst = 0b0001_0100_0000_0001; // mod t0, t1
        computer.execInstruction(modInst);

        assertEquals(0, computer.getRegister(T0));
        assertEquals(5, computer.getRegister(T1));
    }

    @Test
    void testAnd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0b110);
        computer.setRegister(T1, 0b011);

        short andInst = 0b0001_0101_0000_0001; // and t0, t1
        computer.execInstruction(andInst);

        assertEquals(0b010, computer.getRegister(T0));
        assertEquals(0b011, computer.getRegister(T1));
    }

    @Test
    void testOr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0b100);
        computer.setRegister(T1, 0b001);

        short orInst = 0b0001_0110_0000_0001; // or t0, t1
        computer.execInstruction(orInst);

        assertEquals(0b101, computer.getRegister(T0));
        assertEquals(0b001, computer.getRegister(T1));
    }

    @Test
    void testXor() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0b101);
        computer.setRegister(T1, 0b011);

        short xorInst = 0b0001_0111_0000_0001; // xor t0, t1
        computer.execInstruction(xorInst);

        assertEquals(0b110, computer.getRegister(T0));
        assertEquals(0b011, computer.getRegister(T1));
    }

    @Test
    void testShiftLeft() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0b0101);
        computer.setRegister(T1, 1);

        short shiftLeftInst = 0b0001_1000_0000_0001; // shl t0, t1
        computer.execInstruction(shiftLeftInst);

        assertEquals(0b01010, computer.getRegister(T0));
        assertEquals(1, computer.getRegister(T1));
    }

    @Test
    void testShiftRight() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0b0101);
        computer.setRegister(T1, 1);

        short shiftLeftInst = 0b0001_1001_0000_0001; // shr t0, t1
        computer.execInstruction(shiftLeftInst);

        assertEquals(0b0010, computer.getRegister(T0));
        assertEquals(1, computer.getRegister(T1));
    }

    @Test
    void testEqualsResultsInOneWhenEqual() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(T1, 1);

        short eqInst = 0b0001_1010_0000_0001; // eq t0, t1
        computer.execInstruction(eqInst);

        assertEquals(1, computer.getRegister(T0));
        assertEquals(1, computer.getRegister(T1));
    }

    @Test
    void testEqualsResultsInZeroWhenNotEqual() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(T1, 2);

        short eqInst = 0b0001_1010_0000_0001; // eq t0, t1
        computer.execInstruction(eqInst);

        assertEquals(0, computer.getRegister(T0));
        assertEquals(2, computer.getRegister(T1));
    }

    @Test
    void testLessThanResultsInOneWhenLessThan() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(T1, 2);

        short lessThanInst = 0b0001_1011_0000_0001; // lt t0, t1
        computer.execInstruction(lessThanInst);

        assertEquals(1, computer.getRegister(T0));
        assertEquals(2, computer.getRegister(T1));
    }

    @Test
    void testLessThanResultsInZeroWhenNotLessThan() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 2);
        computer.setRegister(T1, 1);

        short lessThanInst = 0b0001_1011_0000_0001; // lt t0, t1
        computer.execInstruction(lessThanInst);

        assertEquals(0, computer.getRegister(T0));
        assertEquals(1, computer.getRegister(T1));
    }

    @Test
    void testLessOrEqualThanResultsInOneWhenLessThan() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(T1, 2);

        short lessThanInst = 0b0001_1100_0000_0001; // lteq t0, t1
        computer.execInstruction(lessThanInst);

        assertEquals(1, computer.getRegister(T0));
        assertEquals(2, computer.getRegister(T1));
    }

    @Test
    void testLessOrEqualThanResultsInOneWhenEqual() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(T1, 1);

        short lessThanInst = 0b0001_1100_0000_0001; // lteq t0, t1
        computer.execInstruction(lessThanInst);

        assertEquals(1, computer.getRegister(T0));
        assertEquals(1, computer.getRegister(T1));
    }

    @Test
    void testLessOrEqualThanResultsInZeroWhenNotLessThanOrEqual() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);
        computer.setRegister(T1, 0);

        short lessThanInst = 0b0001_1100_0000_0001; // lteq t0, t1
        computer.execInstruction(lessThanInst);

        assertEquals(0, computer.getRegister(T0));
        assertEquals(0, computer.getRegister(T1));
    }

    @Test
    void testBitwiseNot() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0b010101);

        short bitwiseNotInst = 0b0001_1101_0000_0000; // bnot t0
        computer.execInstruction(bitwiseNotInst);

        assertEquals(~0b010101, computer.getRegister(T0));
    }

    @Test
    void testNotWithZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 0);

        short notInst = 0b0001_1110_0000_0000; // not t0
        computer.execInstruction(notInst);

        assertEquals(1, computer.getRegister(T0));
    }

    @Test
    void testNotWithNonZeros() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 1);

        short notInst = 0b0001_1110_0000_0000; // not t0
        computer.execInstruction(notInst);
        assertEquals(0, computer.getRegister(T0));

        computer.setRegister(T0, 10);
        computer.execInstruction(notInst);
        assertEquals(0, computer.getRegister(T0));
    }

    @Test
    void testNegate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegister(T0, 10);

        short negInst = 0b0001_1111_0000_0000; // neg t0
        computer.execInstruction(negInst);

        assertEquals(-10, computer.getRegister(T0));
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
