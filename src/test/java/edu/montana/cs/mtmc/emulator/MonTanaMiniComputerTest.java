package edu.montana.cs.mtmc.emulator;

import org.junit.jupiter.api.Test;

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

        short addInst = 0b0010_0000_0000_1011; // push t0, sp
        computer.execInstruction(addInst);

        int newStackAddress = MonTanaMiniComputer.FRAME_BUFF_START - MonTanaMiniComputer.WORD_SIZE;
        assertEquals(newStackAddress, computer.getRegister(SP));
        assertEquals(5, computer.fetchWord(newStackAddress));
    }

    @Test
    void testLoadImmediate() {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();

        short addInst = (short) 0b1000_0000_0000_0101; // ldi t0, 5
        computer.execInstruction(addInst);

        assertEquals(5, computer.getRegister(T0));
    }

}
