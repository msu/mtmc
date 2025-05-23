package mtmc.asm;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.SysCall;
import mtmc.util.BinaryUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static mtmc.emulator.Register.*;
import static org.junit.jupiter.api.Assertions.*;

public class AssemblerTest {

    @Test
    public void bootstrapAssembly() {
        MonTanaMiniComputer computer = assemble("sys exit");
        computer.run();
        assertEquals(computer.getStatus(), MonTanaMiniComputer.ComputerStatus.FINISHED);
    }

    @Test
    public void sysCalls() {
        SysCall[] syscalls = SysCall.values();
        for (SysCall sysCall : syscalls) {
            Assembler assembler = new Assembler();
            byte value = sysCall.getValue();
            AssemblyResult result = assembler.assemble("sys " + sysCall);
            assertArrayEquals(new byte[]{0b0000_0000, value}, result.code());
        }
    }

    @Test
    public void mov() {
        var computer = assemble("mov t0 t1");
        computer.setRegisterValue(T1, 20);
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
    }

    @Test
    public void incNoArg() {
        var computer = assemble("inc t0");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 1);
    }

    @Test
    public void incWithArg() {
        var computer = assemble("inc t0 4");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 4);
    }

    @Test
    public void decNoArg() {
        var computer = assemble("dec t0");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), -1);
    }

    @Test
    public void decWithArg() {
        var computer = assemble("dec t0 4");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), -4);
    }

    @Test
    public void seti() {
        var computer = assemble("seti t1 12");
        assertEquals(computer.getRegisterValue(T1), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 12);
    }

    @Test
    public void noop() {
        var computer = assemble("nop");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 0);
    }

    @Test
    public void add() {
        var computer = assemble("add t0 t1");
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 30);
    }

    @Test
    public void max() {
        var computer = assemble("max t0 t1");
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
    }

    @Test
    public void mod() {
        var computer = assemble("mod t0 t1");
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 10);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 0);
        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    public void mod2() {
        var computer = assemble("mod t0 t1");
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 21);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void neg() {
        var computer = assemble("neg t0");
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), -20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void immAdd() {
        var computer = assemble("imm add t0 10");
        computer.setRegisterValue(T0, 10);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void immAddAsVirtualInst() {
        var computer = assemble("addi t0 10");
        computer.setRegisterValue(T0, 10);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void pushPop() {
        var computer = assemble("""
                push t0
                pop t1
                """);
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 20);
    }

    @Test
    public void pushPopWithCustomStackReg() {
        var computer = assemble("""
                push t0 t5
                pop t1 t5
                """);
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T5, 100);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 20);
    }

    @Test
    public void pushWithCustomStackReg() {
        var computer = assemble("""
                push t0 t5
                """);
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T5, 100);
        computer.run();
        assertEquals(computer.fetchWordFromMemory(98), 20);
    }

    @Test
    public void dup() {
        var computer = assemble("""
                push t0
                dup
                pop t1
                pop t2
                """);
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 20);
        assertEquals(computer.getRegisterValue(T2), 20);
    }

    @Test
    public void sop() {
        var computer = assemble("""
                push t0
                dup
                sop add
                pop t1
                """);
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 40);
    }

    @Test
    public void sadd() {
        var computer = assemble("""
                push t0
                dup
                sadd
                pop t1
                """);
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 40);
    }

    @Test
    public void pushi() {
        var computer = assemble("""
                pushi 1000
                pop t1
                """);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 1000);
    }

    @Test
    public void eq() {
        var computer = assemble("""
                seti t0 1
                seti t1 2
                eq t0 t1
                """);
        computer.run();
        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    public void neq() {
        var computer = assemble("""
                seti t0 1
                seti t1 2
                neq t0 t1
                """);
        computer.run();
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void eqi() {
        var computer = assemble("""
                seti t0 1
                eqi t0 1
                """);
        computer.run();
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void neqi() {
        var computer = assemble("""
                seti t0 1
                neqi t0 1
                """);
        computer.run();
        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    public void lwr() {
        var computer = assemble("""
                lwr t0 t1 t2
                """);
        computer.setRegisterValue(T1, 20);
        computer.setRegisterValue(T2, 20);
        computer.writeWordToMemory(40, 100);
        computer.run();
        assertEquals(100, computer.getRegisterValue(T0));
    }

    @Test
    public void swr() {
        var computer = assemble("""
                swr t0 t1 t2
                """);
        computer.setRegisterValue(T0, 100);
        computer.setRegisterValue(T1, 20);
        computer.setRegisterValue(T2, 20);
        computer.run();
        assertEquals(100, computer.fetchWordFromMemory(40));
    }

    @Test
    public void lw() {
        var computer = assemble("""
                .data
                  foo: 100
                .text
                  lw t0 foo
                """);
        computer.run();
        assertEquals(100, computer.getRegisterValue(T0));
    }

    @Test
    public void lwAbsolute() {
        var computer = assemble("""
                  lw t0 20
                """);
        computer.writeWordToMemory(20, 100);
        computer.run();
        assertEquals(100, computer.getRegisterValue(T0));
    }

    @Test
    public void lwo() {
        var computer = assemble("""
                .data
                  foo: 100
                  bar: 200
                .text
                  seti t1 2
                  lwo t0 t1 foo
                """);
        computer.run();
        assertEquals(200, computer.getRegisterValue(T0));
    }

    @Test
    public void li() {
        var computer = assemble("""
                li t0 100
                """);
        computer.run();
        assertEquals(100, computer.getRegisterValue(T0));
    }

    @Test
    public void la() {
        var computer = assemble("""
                .data
                foo: 100
                .text
                la t0 foo
                """);
        computer.run();
        assertTrue(computer.getRegisterValue(T0) > 0);
    }

    @Test
    public void jr() {
        var computer = assemble("""
                seti t0 6 # jump immediately after the jr instruction (exit)
                jr t0
                """);
        computer.run();
        assertEquals(8, computer.getRegisterValue(PC));
    }

    @Test
    public void j() {
        var computer = assemble("""
                j 6
                """);
        computer.run();
        assertEquals(8, computer.getRegisterValue(PC));
    }

    @Test
    public void jWithLabel() {
        var computer = assemble("""
                start: j foo
                j start
                foo: sys exit
                """);
        computer.run();
        assertEquals(6, computer.getRegisterValue(PC));
    }

    @Test
    public void jz() {
        var computer = assemble("""
                seti t0 2
                modi t0 2
                jz end
                seti t1 1
                end: sys exit
                """);
        computer.run();
        assertEquals(0, computer.getRegisterValue(T1));
    }

    @Test
    public void jnz() {
        var computer = assemble("""
                seti t0 2
                modi t0 2
                jnz end
                seti t1 1
                end: sys exit
                """);
        computer.run();
        assertEquals(1, computer.getRegisterValue(T1));
    }

    @Test
    public void jal() {
        var computer = assemble("""
                nop
                jal end
                end: sys exit
                """);
        computer.run();
        assertEquals(6, computer.getRegisterValue(PC));
        assertEquals(4, computer.getRegisterValue(RA));
    }

    @Test
    public void endToEndMax() {
        var computer = assemble("""
                pushi 1
                pushi 2
                smax
                pop t0
                """);
        computer.run();
        assertEquals(2, computer.getRegisterValue(T0));
        assertEquals(MonTanaMiniComputer.MEMORY_SIZE, computer.getRegisterValue(SP));
    }


    //--------------------------------------
    // helpers
    //--------------------------------------
    private static void assertBytesAtOffsetAre(byte[] bytes, int offset, int... values) {
        for (int i = 0; i < values.length; i++) {
            byte expected = (byte) values[i];
            if (bytes.length < offset + i) {
                fail("Not enough bytes in array : " + toString(bytes));
            }
            byte byteValue = bytes[offset + i];
            assertEquals(expected, byteValue, "The " + offset + i + " byte is wrong," +
                    " expected " + BinaryUtils.toBinary(expected) + " but found " + BinaryUtils.toBinary(byteValue));
        }
    }

    private static String toString(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            byte aByte = bytes[i];
            sb.append(BinaryUtils.toBinary(aByte));
            if (i < bytes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @NotNull
    private static MonTanaMiniComputer assemble(String assembly) {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble(assembly);
        if (result.errors().isEmpty()) {
            MonTanaMiniComputer computer = new MonTanaMiniComputer();
            computer.load(result.code(), result.data());
            return computer;
        } else {
            throw new RuntimeException("Assembly errors: \n\n" + result.printErrors());
        }
    }
}
