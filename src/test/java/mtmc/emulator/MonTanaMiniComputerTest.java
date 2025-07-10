package mtmc.emulator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static mtmc.emulator.MonTanaMiniComputer.WORD_SIZE;
import static mtmc.emulator.Register.*;
import static org.junit.jupiter.api.Assertions.*;

public class MonTanaMiniComputerTest {

    @Test
    void testSysCall() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        assertEquals(computer.getStatus(), MonTanaMiniComputer.ComputerStatus.READY);
        short haltInst = 0b0000_0000_0000_0000; // sys halt
        computer.execInstruction(haltInst);
        assertEquals(computer.getStatus(), MonTanaMiniComputer.ComputerStatus.FINISHED);
    }

    @Test
    void testMov() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 0);

        short moveInst = 0b0000_0001_0001_0000; // mov t1 t0
        computer.execInstruction(moveInst);

        assertEquals(10, computer.getRegisterValue(T0));
        assertEquals(10, computer.getRegisterValue(T1));
    }

    @Test
    void testInc() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);
        short incInst = 0b0000_0010_0000_0001; // inc t0
        computer.execInstruction(incInst);
        assertEquals(1, computer.getRegisterValue(T0));
    }

    @Test
    void testInc3() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);
        short incInst = 0b0000_0010_0000_0011; // inc t0
        computer.execInstruction(incInst);
        assertEquals(3, computer.getRegisterValue(T0));
    }

    @Test
    void testDec() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);
        short decInst = 0b0000_0011_0000_0001; // Dec t0
        computer.execInstruction(decInst);
        assertEquals(-1, computer.getRegisterValue(T0));
    }

    @Test
    void testDec3() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);
        short decInst = 0b0000_0011_0000_0011; // Dec t0
        computer.execInstruction(decInst);
        assertEquals(-3, computer.getRegisterValue(T0));
    }

    @Test
    void testSeti() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);
        short inst = 0b0000_0100_0000_0011; // seti t0 3
        computer.execInstruction(inst);
        assertEquals(3, computer.getRegisterValue(T0));
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
        computer.setRegisterValue(T0, 5);
        computer.setRegisterValue(T1, 10);

        short addInst = 0b0001_0000_0000_0001; // add t0, t1
        computer.execInstruction(addInst);

        assertEquals(15, computer.getRegisterValue(T0));
        assertEquals(10, computer.getRegisterValue(T1));
    }

    @Test
    void testSub() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 5);

        short subInst = 0b0001_0001_0000_0001; // sub t0, t1
        computer.execInstruction(subInst);

        assertEquals(5, computer.getRegisterValue(T0));
        assertEquals(5, computer.getRegisterValue(T1));
    }

    @Test
    void testMul() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 5);

        short mulInst = 0b0001_0010_0000_0001; // mul t0, t1
        computer.execInstruction(mulInst);

        assertEquals(50, computer.getRegisterValue(T0));
        assertEquals(5, computer.getRegisterValue(T1));
    }

    @Test
    void testDiv() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 5);

        short divInst = 0b0001_0011_0000_0001; // div t0, t1
        computer.execInstruction(divInst);

        assertEquals(2, computer.getRegisterValue(T0));
        assertEquals(5, computer.getRegisterValue(T1));
    }

    @Test
    void testMod() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 5);

        short modInst = 0b0001_0100_0000_0001; // mod t0, t1
        computer.execInstruction(modInst);

        assertEquals(0, computer.getRegisterValue(T0));
        assertEquals(5, computer.getRegisterValue(T1));
    }

    @Test
    void testAnd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0b110);
        computer.setRegisterValue(T1, 0b011);

        short andInst = 0b0001_0101_0000_0001; // and t0, t1
        computer.execInstruction(andInst);

        assertEquals(0b010, computer.getRegisterValue(T0));
        assertEquals(0b011, computer.getRegisterValue(T1));
    }

    @Test
    void testOr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0b100);
        computer.setRegisterValue(T1, 0b001);

        short orInst = 0b0001_0110_0000_0001; // or t0, t1
        computer.execInstruction(orInst);

        assertEquals(0b101, computer.getRegisterValue(T0));
        assertEquals(0b001, computer.getRegisterValue(T1));
    }

    @Test
    void testXor() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0b101);
        computer.setRegisterValue(T1, 0b011);

        short xorInst = 0b0001_0111_0000_0001; // xor t0, t1
        computer.execInstruction(xorInst);

        assertEquals(0b110, computer.getRegisterValue(T0));
        assertEquals(0b011, computer.getRegisterValue(T1));
    }

    @Test
    void testShiftLeft() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0b0101);
        computer.setRegisterValue(T1, 1);

        short shiftLeftInst = 0b0001_1000_0000_0001; // shl t0, t1
        computer.execInstruction(shiftLeftInst);

        assertEquals(0b01010, computer.getRegisterValue(T0));
        assertEquals(1, computer.getRegisterValue(T1));
    }

    @Test
    void testShiftRight() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0b0101);
        computer.setRegisterValue(T1, 1);

        short shiftLeftInst = 0b0001_1001_0000_0001; // shr t0, t1
        computer.execInstruction(shiftLeftInst);

        assertEquals(0b0010, computer.getRegisterValue(T0));
        assertEquals(1, computer.getRegisterValue(T1));
    }

    @Test
    void testMin() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 10);

        short minInst = 0b0001_1010_0000_0001; // min t0, t1
        computer.execInstruction(minInst);

        assertEquals(10, computer.getRegisterValue(T0));
        assertEquals(10, computer.getRegisterValue(T1));
    }

    @Test
    void testMin2() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 20);

        short minInst = 0b0001_1010_0000_0001; // min t0, t1
        computer.execInstruction(minInst);

        assertEquals(10, computer.getRegisterValue(T0));
        assertEquals(20, computer.getRegisterValue(T1));
    }

    @Test
    void testMax() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 10);

        short maxInst = 0b0001_1011_0000_0001; // max t0, t1
        computer.execInstruction(maxInst);

        assertEquals(20, computer.getRegisterValue(T0));
        assertEquals(10, computer.getRegisterValue(T1));
    }

    @Test
    void testMax2() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 20);

        short maxInst = 0b0001_1011_0000_0001; // max t0, t1
        computer.execInstruction(maxInst);

        assertEquals(20, computer.getRegisterValue(T0));
        assertEquals(20, computer.getRegisterValue(T1));
    }


    @Test
    void testBitwiseNot() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0b010101);

        short bitwiseNotInst = 0b0001_1100_0000_0000; // bnot t0
        computer.execInstruction(bitwiseNotInst);

        assertEquals(~0b010101, computer.getRegisterValue(T0));
    }

    @Test
    void testNotWithZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);

        short notInst = 0b0001_1101_0000_0000; // not t0
        computer.execInstruction(notInst);

        assertEquals(1, computer.getRegisterValue(T0));
    }

    @Test
    void testNotWithNonZeros() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);

        short notInst = 0b0001_1101_0000_0000; // not t0
        computer.execInstruction(notInst);
        assertEquals(0, computer.getRegisterValue(T0));

        computer.setRegisterValue(T0, 10);
        computer.execInstruction(notInst);
        assertEquals(0, computer.getRegisterValue(T0));
    }

    @Test
    void testNegate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);

        short negInst = 0b0001_1110_0000_0000; // neg t0
        computer.execInstruction(negInst);

        assertEquals(-10, computer.getRegisterValue(T0));
    }

    @Test
    void testImmediateInstruction() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(DR, 10);

        short negInst = 0b0001_1111_0000_0000; // imm add t0 10
        computer.execInstruction(negInst);

        assertEquals(20, computer.getRegisterValue(T0));
    }

    @Test
    void testPush() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 5);

        short pushInst = 0b0010_0000_0000_1101; // push t0, sp
        computer.execInstruction(pushInst);

        int newStackAddress = MonTanaMiniComputer.MEMORY_SIZE - WORD_SIZE;
        assertEquals(newStackAddress, computer.getRegisterValue(SP));
        assertEquals(5, computer.fetchWordFromMemory(newStackAddress));
    }

    @Test
    void testPop() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 0);
        computer.setRegisterValue(SP, 100);
        computer.writeWordToMemory(100, (short) 10);

        short popInst = 0b0010_0001_0000_1101; // pop t0, sp
        computer.execInstruction(popInst);

        assertEquals(10, computer.getRegisterValue(T0));
        assertEquals(102, computer.getRegisterValue(SP));
    }

    @Test
    void testDuplicate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 100);
        computer.writeWordToMemory(100, (short) 10);

        short duplicateInst = 0b0010_0010_0000_1101; // dup sp
        computer.execInstruction(duplicateInst);

        assertEquals(98, computer.getRegisterValue(SP));
        assertEquals(10, computer.fetchWordFromMemory(98));
    }

    @Test
    void testSwap() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 10);
        computer.writeWordToMemory(98, (short) 20);

        short swapIns = 0b0010_0011_0000_1101; // swap sp
        computer.execInstruction(swapIns);

        assertEquals(98, computer.getRegisterValue(SP));
        assertEquals(10, computer.fetchWordFromMemory(98));
        assertEquals(20, computer.fetchWordFromMemory(100));
    }

    @Test
    void testDrop() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 10);
        computer.writeWordToMemory(98, (short) 20);

        short dropInst = 0b0010_0100_0000_1101; // swap sp
        computer.execInstruction(dropInst);

        assertEquals(100, computer.getRegisterValue(SP));
    }

    @Test
    void testOver() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 10);
        computer.writeWordToMemory(98, (short) 20);

        short overInst = 0b0010_0101_0000_1101; // over sp
        computer.execInstruction(overInst);

        assertEquals(96, computer.getRegisterValue(SP));
        assertEquals(10, computer.fetchWordFromMemory(100));
        assertEquals(20, computer.fetchWordFromMemory(98));
        assertEquals(10, computer.fetchWordFromMemory(96));
    }

    @Test
    void testRot() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 96);
        computer.writeWordToMemory(100, (short) 10);
        computer.writeWordToMemory(98, (short) 20);
        computer.writeWordToMemory(96, (short) 30);

        short rotInst = 0b0010_0110_0000_1101; // over sp
        computer.execInstruction(rotInst);

        assertEquals(96, computer.getRegisterValue(SP));
        assertEquals(20, computer.fetchWordFromMemory(100));
        assertEquals(30, computer.fetchWordFromMemory(98));
        assertEquals(10, computer.fetchWordFromMemory(96));
    }

    @Test
    void testSopAdd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 10);
        computer.writeWordToMemory(98, (short) 20);

        short sopAddInst = 0b0010_0111_0000_1101; // sop add sp
        computer.execInstruction(sopAddInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(30, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopSub() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 20);
        computer.writeWordToMemory(98, (short) 10);

        short sopSubInst = 0b0010_0111_0001_1101; // sop sub sp
        computer.execInstruction(sopSubInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(10, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopMul() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 20);
        computer.writeWordToMemory(98, (short) 10);

        short sopMulInst = 0b0010_0111_0010_1101; // sop mul sp
        computer.execInstruction(sopMulInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(200, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopDiv() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 20);
        computer.writeWordToMemory(98, (short) 10);

        short sopDivInst = 0b0010_0111_0011_1101; // sop div sp
        computer.execInstruction(sopDivInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(2, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopMod() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 20);
        computer.writeWordToMemory(98, (short) 10);

        short sopModInst = 0b0010_0111_0100_1101; // sop mod sp
        computer.execInstruction(sopModInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(0, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopAnd() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 0b110);
        computer.writeWordToMemory(98, (short) 0b011);

        short sopAndInst = 0b0010_0111_0101_1101; // sop and sp
        computer.execInstruction(sopAndInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(0b010, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopOr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 0b100);
        computer.writeWordToMemory(98, (short) 0b001);

        short sopOrInst = 0b0010_0111_0110_1101; // sop or sp
        computer.execInstruction(sopOrInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(0b101, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopXor() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 0b110);
        computer.writeWordToMemory(98, (short) 0b011);

        short sopXorInst = 0b0010_0111_0111_1101; // sop xor sp
        computer.execInstruction(sopXorInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(0b101, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopShl() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 0b110);
        computer.writeWordToMemory(98, (short) 1);

        short sopShlInst = 0b0010_0111_1000_1101; // sop shl sp
        computer.execInstruction(sopShlInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(0b1100, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopShr() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 0b110);
        computer.writeWordToMemory(98, (short) 1);

        short sopShrInst = 0b0010_0111_1001_1101; // sop shr sp
        computer.execInstruction(sopShrInst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(0b011, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSopMin() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 10);
        computer.writeWordToMemory(98, (short) 20);

        short inst = 0b0010_0111_1010_1101; // sop min sp
        computer.execInstruction(inst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(10, computer.fetchWordFromMemory(computer.getRegisterValue(SP)));
    }

    @Test
    void testSopMin2() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(98, (short) 20);
        computer.writeWordToMemory(100, (short) 10);

        short inst = 0b0010_0111_1010_1101; // sop min sp
        computer.execInstruction(inst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(10, computer.fetchWordFromMemory(computer.getRegisterValue(SP)));
    }

    @Test
    void testSopMax() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(100, (short) 20);
        computer.writeWordToMemory(98, (short) 10);

        short inst = 0b0010_0111_1011_1101; // sop max sp
        computer.execInstruction(inst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(20, computer.fetchWordFromMemory(computer.getRegisterValue(SP)));
    }

    @Test
    void testSopMax2() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(98, (short) 10);
        computer.writeWordToMemory(100, (short) 20);

        short inst = 0b0010_0111_1011_1101; // sop max sp
        computer.execInstruction(inst);

        assertEquals(100, computer.getRegisterValue(SP));
        assertEquals(20, computer.fetchWordFromMemory(computer.getRegisterValue(SP)));
    }

    @Test
    void testSopNot() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(98, (short) 0b01010101);

        short sopBnotInst = 0b0010_0111_1100_1101; // sop not sp
        computer.execInstruction(sopBnotInst);

        assertEquals(98, computer.getRegisterValue(SP));
        assertEquals(~0b01010101, computer.fetchWordFromMemory(98));
    }

    @Test
    void testSopLnotWhenNonZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(98, (short) 1);

        short sopNotInst = 0b0010_0111_1101_1101; // sop lnot sp
        computer.execInstruction(sopNotInst);

        assertEquals(98, computer.getRegisterValue(SP));
        assertEquals(0, computer.fetchWordFromMemory(98));
    }

    @Test
    void testSopLnotWhenZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(98, (short) 0);

        short sopNotInst = 0b0010_0111_1101_1101; // sop not sp
        computer.execInstruction(sopNotInst);

        assertEquals(98, computer.getRegisterValue(SP));
        assertEquals(1, computer.fetchWordFromMemory(98));
    }

    @Test
    void testSopNeg() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(SP, 98);
        computer.writeWordToMemory(98, (short) 10);

        short sopNegInst = 0b0010_0111_1110_1101; // sop neg sp
        computer.execInstruction(sopNegInst);

        assertEquals(98, computer.getRegisterValue(SP));
        assertEquals(-10, computer.fetchWordFromMemory(98));
    }


    @Test
    void testPushImmediate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 1024);

        short inst = (short) 0b0010_1111_0000_1101; // pushi sp 1024
        computer.execInstruction(inst);

        assertEquals(1024, computer.fetchWordFromMemory(computer.getRegisterValue(SP)));
    }

    @Test
    void testEqWhenEq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1024);
        computer.setRegisterValue(T1, 1024);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0000_0000_0001; // eq t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testEqWhenNotEq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1024);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0000_0000_0001; // eq t0 t1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testNeqWhenNeq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1024);
        computer.setRegisterValue(T1, 1024);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0001_0000_0001; // neq t0 t1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testNeqWhenNotNeq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1024);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0001_0000_0001; // neq t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }
    
    @Test
    void testGtWhenGt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        computer.setRegisterValue(T1, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0010_0000_0001; // gt t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGtWhenNotGt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0010_0000_0001; // gt t0 t1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testGteWhenGt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        computer.setRegisterValue(T1, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0011_0000_0001; // gte t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGteWhenE() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0011_0000_0001; // gte t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGteWhenNotGte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0011_0000_0001; // gte t0 t1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testLtWhenLt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0100_0000_0001; // lt t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testLtWhenNotLt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        computer.setRegisterValue(T1, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0100_0000_0001; // lt t0 t1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testLteWhenLt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0101_0000_0001; // lte t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testLteWhenE() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        computer.setRegisterValue(T1, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0101_0000_0001; // lte t0 t1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testLteWhenNotLte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        computer.setRegisterValue(T1, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_0101_0000_0001; // lte t0 t1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testEqiWhenEq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1000_0000_0001; // eqi t0 1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testEqiWhenNotEq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1024);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1000_0000_0001; // eqi t0 1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testNeqiWhenNeq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1001_0000_0001; // neqi t0 1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testNeqiWhenNotNeq() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1001_0000_0001; // neqi t0 1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGtiWhenGt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1010_0000_0001; // gti t0 1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGtiWhenNotGt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1010_0000_0001; // gti t0 1
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testGteiWhenGt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1011_0000_0001; // gte t0 1
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGteiWhenE() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1011_0000_0010; // gtei t0 2
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testGteiWhenNotGte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1011_0000_0010; // gtei t0 2
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testLtiWhenLt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1100_0000_0010; // lti t0 2
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testLtiWhenNotLt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1100_0000_0010; // lti t0 2
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testLteiWhenLt() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 1);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1101_0000_0010; // ltei t0 2
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testLteiWhenE() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 2);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1101_0000_0010; // ltei t0 2
        computer.execInstruction(inst);

        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    void testLteiWhenNotLte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 3);
        assertFalse(computer.isFlagTestBitSet());

        short inst = (short) 0b0011_1101_0000_0010; // ltei t0 2
        computer.execInstruction(inst);

        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    void testLoadWord() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.writeWordToMemory(10, 20);
        assertEquals(0, computer.getRegisterValue(T0));

        short inst = (short) 0b1000_0000_0000_0000; // lw t0 10
        computer.execInstruction(inst);

        assertEquals(20, computer.getRegisterValue(T0));
    }

    @Test
    void testLoadWordOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.setRegisterValue(T1, 5);
        computer.writeWordToMemory(15, 20);
        assertEquals(0, computer.getRegisterValue(T0));

        short inst = (short) 0b1000_0001_0000_0001; // lwo t0 t1 10
        computer.execInstruction(inst);

        assertEquals(20, computer.getRegisterValue(T0));
    }

    @Test
    void testLoadByte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.writeByteToMemory(10, (byte) 20);
        assertEquals(0, computer.getRegisterValue(T0));

        short inst = (short) 0b1000_0010_0000_0000; // lw t0 10
        computer.execInstruction(inst);

        assertEquals(20, computer.getRegisterValue(T0));
    }

    @Test
    void testLoadByteOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.setRegisterValue(T1, 5);
        computer.writeByteToMemory(15, (byte) 20);
        assertEquals(0, computer.getRegisterValue(T0));

        short inst = (short) 0b1000_0011_0000_0001; // lwo t0 t1 10
        computer.execInstruction(inst);

        assertEquals(20, computer.getRegisterValue(T0));
    }

    @Test
    void testSaveWord() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.setRegisterValue(T0, 20);
        assertEquals(0, computer.fetchWordFromMemory(10));

        short inst = (short) 0b1000_0100_0000_0000; // sw t0 10
        computer.execInstruction(inst);

        assertEquals(20, computer.fetchWordFromMemory(10));
    }

    @Test
    void testSaveWordWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 10);
        assertEquals(0, computer.fetchWordFromMemory(20));

        short inst = (short) 0b1000_0101_0000_0001; // swo t0 t1 10
        computer.execInstruction(inst);

        assertEquals(20, computer.fetchWordFromMemory(20));
    }

    @Test
    void testSaveByte() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.setRegisterValue(T0, 20);
        assertEquals(0, computer.fetchByteFromMemory(10));

        short inst = (short) 0b1000_0110_0000_0000; // sw t0 10
        computer.execInstruction(inst);

        assertEquals(20, computer.fetchByteFromMemory(10));
    }

    @Test
    void testSaveByteWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 10);
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 10);
        assertEquals(0, computer.fetchByteFromMemory(20));

        short inst = (short) 0b1000_0111_0000_0001; // swo t0 t1 10
        computer.execInstruction(inst);

        assertEquals(20, computer.fetchByteFromMemory(20));
    }

    @Test
    void testLoadImmediate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(DR, 20);
        assertEquals(0, computer.getRegisterValue(T0));

        short inst = (short) 0b1000_1111_0000_0001; // li t0 t1 10
        computer.execInstruction(inst);

        assertEquals(20, computer.getRegisterValue(T0));
    }

    @Test
    void testLoadRegisterWord() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 0);
        computer.writeWordToMemory(100, (short) 10); // value written to memory

        short loadWordInst = (short) 0b0100_0000_0001_0010; // lw t0, t1, t2
        computer.execInstruction(loadWordInst);

        assertEquals(10, computer.getRegisterValue(T0)); // should be loaded into t0
    }

    @Test
    void testLoadWordRegisterWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 5);
        computer.writeWordToMemory(105, (short) 10); // value written to memory

        short loadWordInst = (short) 0b0100_0000_0001_0010; // lw t0, t1, t2
        computer.execInstruction(loadWordInst);

        assertEquals(10, computer.getRegisterValue(T0)); // should be loaded into t0
    }

    @Test
    void testLoadByteRegister() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 0);
        computer.writeByteToMemory(100, (byte) 10); // value written to memory

        short loadByteInst = (short) 0b0101_0000_0001_0010; // lb t0, t1, t2
        computer.execInstruction(loadByteInst);

        assertEquals(10, computer.getRegisterValue(T0)); // should be loaded into t0
    }

    @Test
    void testLoadByteRegisterWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 5);
        computer.writeByteToMemory(105, (byte) 10); // value written to memory

        short loadByteInst = (short) 0b0101_0000_0001_0010; // lb t0, t1, t2
        computer.execInstruction(loadByteInst);

        assertEquals(10, computer.getRegisterValue(T0)); // should be loaded into t0
    }

    @Test
    void testSaveRegisterWord() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 0);

        short saveWordInst = (short) 0b0110_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveWordInst);

        assertEquals(10, computer.fetchWordFromMemory(100));
    }

    @Test
    void testSaveWordRegisterWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 5);

        short saveWordInst = (short) 0b0110_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveWordInst);

        assertEquals(10, computer.fetchWordFromMemory(105));
    }

    @Test
    void testSaveByteRegister() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 0);

        short saveByteInst = (short) 0b0111_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveByteInst);

        assertEquals(10, computer.fetchByteFromMemory(100));
    }

    @Test
    void testSaveByteRegisterWithOffset() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 100);
        computer.setRegisterValue(T2, 5);

        short saveByteInst = (short) 0b0111_0000_0001_0010; // sw t0, t1, t2
        computer.execInstruction(saveByteInst);

        assertEquals(10, computer.fetchByteFromMemory(105));
    }


    @Test
    void testJumpRegister() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(T0, 50);
        short jumpIfZeroInst = (short) 0b1001_0000_0000_0000; // jr t0
        computer.execInstruction(jumpIfZeroInst);
        assertEquals(50, computer.getRegisterValue(PC));
    }

    @Test
    void testJump() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(PC, 50);

        short jumpInst = (short) 0b1100_0000_0001_0000; // j 16
        computer.execInstruction(jumpInst);

        assertEquals(16, computer.getRegisterValue(PC));
    }

    @Test
    void testJumpZeroIfFlagBitSetToZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setFlagTestBit(false);
        short jumpIfZeroInst = (short) 0b1101_0000_0001_0000; // jz 16
        computer.execInstruction(jumpIfZeroInst);
        assertEquals(16, computer.getRegisterValue(PC));
    }

    @Test
    void testJumpZeroIfFlagBitSetToOne() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setFlagTestBit(true);
        short jumpIfZeroInst = (short) 0b1101_0000_0001_0000; // jz 16
        computer.execInstruction(jumpIfZeroInst);
        assertEquals(0, computer.getRegisterValue(PC));
    }

    @Test
    void testJumpNotZeroIfFlagBitSetToZero() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setFlagTestBit(false);
        short jumpIfZeroInst = (short) 0b1110_0000_0001_0000; // jnz 16
        computer.execInstruction(jumpIfZeroInst);
        assertEquals(0, computer.getRegisterValue(PC));
    }

    @Test
    void testJumpNotZeroIfFlagBitSetToOne() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setFlagTestBit(true);
        short jumpIfZeroInst = (short) 0b1110_0000_0001_0000; // jnz 16
        computer.execInstruction(jumpIfZeroInst);
        assertEquals(16, computer.getRegisterValue(PC));
    }

    @Test
    void testJumpAndLink() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setRegisterValue(PC, 50);

        short callInst = (short) 0b1111_0000_0001_0000; // jal 16
        computer.execInstruction(callInst);

        assertEquals(16, computer.getRegisterValue(PC));
        assertEquals(50, computer.getRegisterValue(RA));
    }


}
