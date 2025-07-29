package mtmc.asm;

import mtmc.emulator.DebugInfo;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.SysCall;
import mtmc.util.BinaryUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static mtmc.emulator.Register.*;
import static org.junit.jupiter.api.Assertions.*;

public class AssemblerTest {

    @Test
    public void bootstrapAssembly() {
        MonTanaMiniComputer computer = assembleAndLoad("sys exit");
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
        var computer = assembleAndLoad("mov t0 t1");
        computer.setRegisterValue(T1, 20);
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
    }

    @Test
    public void incNoArg() {
        var computer = assembleAndLoad("inc t0");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 1);
    }

    @Test
    public void incWithArg() {
        var computer = assembleAndLoad("inc t0 4");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 4);
    }

    @Test
    public void decNoArg() {
        var computer = assembleAndLoad("dec t0");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), -1);
    }

    @Test
    public void decWithArg() {
        var computer = assembleAndLoad("dec t0 4");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), -4);
    }

    @Test
    public void seti() {
        var computer = assembleAndLoad("seti t1 12");
        assertEquals(computer.getRegisterValue(T1), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 12);
    }

    @Test
    public void memcpy() {
        var computer = assembleAndLoad("mcp t0 t1 8");
        computer.writeWordToMemory(100, 0x0123);
        computer.writeWordToMemory(102, 0x4567);
        computer.writeWordToMemory(104, 0x89AB);
        computer.writeWordToMemory(106, 0xCDEF);
        computer.setRegisterValue(Register.T0, 100);
        computer.setRegisterValue(Register.T1, 108);
        computer.run();
        assertEquals((short) 0x0123, computer.fetchWordFromMemory(108));
        assertEquals((short) 0x4567, computer.fetchWordFromMemory(110));
        assertEquals((short) 0x89AB, computer.fetchWordFromMemory(112));
        assertEquals((short) 0xCDEF, computer.fetchWordFromMemory(114));
    }

    @Test
    public void noop() {
        var computer = assembleAndLoad("nop");
        assertEquals(computer.getRegisterValue(T0), 0);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 0);
    }

    @Test
    public void add() {
        var computer = assembleAndLoad("add t0 t1");
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 30);
    }

    @Test
    public void max() {
        var computer = assembleAndLoad("max t0 t1");
        computer.setRegisterValue(T0, 10);
        computer.setRegisterValue(T1, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
    }

    @Test
    public void mod() {
        var computer = assembleAndLoad("mod t0 t1");
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 10);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 0);
        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    public void mod2() {
        var computer = assembleAndLoad("mod t0 t1");
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T1, 21);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void neg() {
        var computer = assembleAndLoad("neg t0");
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), -20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void immAdd() {
        var computer = assembleAndLoad("imm add t0 10");
        computer.setRegisterValue(T0, 10);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void immAddAsVirtualInst() {
        var computer = assembleAndLoad("addi t0 10");
        computer.setRegisterValue(T0, 10);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void pushPop() {
        var computer = assembleAndLoad("""
                push t0
                pop t1
                """);
        computer.setRegisterValue(T0, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 20);
    }

    @Test
    public void pushPopRv() {
        var computer = assembleAndLoad("""
                push rv
                pop t0
                """);
        computer.setRegisterValue(RV, 20);
        computer.run();
        assertEquals(computer.getRegisterValue(T0), 20);
    }

    @Test
    public void pushPopWithCustomStackReg() {
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
                push t0 t5
                """);
        computer.setRegisterValue(T0, 20);
        computer.setRegisterValue(T5, 100);
        computer.run();
        assertEquals(computer.fetchWordFromMemory(98), 20);
    }

    @Test
    public void dup() {
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
                pushi 1000
                pop t1
                """);
        computer.run();
        assertEquals(computer.getRegisterValue(T1), 1000);
    }

    @Test
    public void eq() {
        var computer = assembleAndLoad("""
                seti t0 1
                seti t1 2
                eq t0 t1
                """);
        computer.run();
        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    public void neq() {
        var computer = assembleAndLoad("""
                seti t0 1
                seti t1 2
                neq t0 t1
                """);
        computer.run();
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void eqi() {
        var computer = assembleAndLoad("""
                seti t0 1
                eqi t0 1
                """);
        computer.run();
        assertTrue(computer.isFlagTestBitSet());
    }

    @Test
    public void neqi() {
        var computer = assembleAndLoad("""
                seti t0 1
                neqi t0 1
                """);
        computer.run();
        assertFalse(computer.isFlagTestBitSet());
    }

    @Test
    public void lwr() {
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
                  lw t0 20
                """);
        computer.writeWordToMemory(20, 100);
        computer.run();
        assertEquals(100, computer.getRegisterValue(T0));
    }

    @Test
    public void lwo() {
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
                li t0 100
                """);
        computer.run();
        assertEquals(100, computer.getRegisterValue(T0));
    }

    @Test
    public void la() {
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
                seti t0 6 # jump immediately after the jr instruction (exit)
                jr t0
                """);
        computer.run();
        assertEquals(8, computer.getRegisterValue(PC));
    }

    @Test
    public void j() {
        var computer = assembleAndLoad("""
                j 6
                """);
        computer.run();
        assertEquals(8, computer.getRegisterValue(PC));
    }

    @Test
    public void jWithLabel() {
        var computer = assembleAndLoad("""
                start: j foo
                j start
                foo: sys exit
                """);
        computer.run();
        assertEquals(6, computer.getRegisterValue(PC));
    }

    @Test
    public void jz() {
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
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
        var computer = assembleAndLoad("""
                pushi 1
                pushi 2
                smax
                pop t0
                """);
        computer.run();
        assertEquals(2, computer.getRegisterValue(T0));
        assertEquals(MonTanaMiniComputer.MEMORY_SIZE, computer.getRegisterValue(SP));
    }

    @Test
    public void assemblyLineNumbersAreCorrect() {
        var result = assemble("""
                pushi 1
                pushi 2
                smax
                pop t0
                """);
        int[] asmLineNums = result.debugInfo().assemblyLineNumbers();
        assertEquals(result.code().length, asmLineNums.length);
        assertArrayEquals(new int[]{1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4}, asmLineNums);
    }

    @Test
    public void originalFileIsCorrect() {
        var result = assemble("""
                @file "foo.sea"
                pushi 1
                pushi 2
                smax
                pop t0
                """);
        assertEquals("foo.sea", result.debugInfo().originalFile());
    }

    @Test
    public void originalLineNumbersAreCorrect() {
        var result = assemble("""
                @file "foo.sea"
                pop t0
                @line 1
                pop t0
                @line 2
                pop t0
                @line 1
                pop t0
                @line 0
                pop t0
                """);
        int[] originalLineNumbers = result.debugInfo().originalLineNumbers();
        assertEquals(result.code().length, originalLineNumbers.length);
        assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 1, 1, 0, 0}, originalLineNumbers);
    }

    @Test
    public void globalsInfoIsCorrect() {
        var result = assemble("""
                @file "foo.sea"
                pushi 1
                @global "foo" 22 "char*"
                pushi 2
                smax
                @global "baz" 11 "int"
                pop t0
                """);
        var globals = result.debugInfo().globals();
        assertEquals(2, globals.length);

        assertEquals("foo", globals[0].name());
        assertEquals(22, globals[0].location());
        assertEquals("char*", globals[0].type());

        assertEquals("baz", globals[1].name());
        assertEquals(11, globals[1].location());
        assertEquals("int", globals[1].type());

    }

    @Test
    public void localsInfoIsCorrect() {
        var result = assemble("""
                @file "foo.sea"
                pop t0
                @local "foo" 0 "char*"
                pop t0
                @local "baz" 1 "int"
                pop t0
                @endlocal "foo"
                pop t0
                """);
        var locals = result.debugInfo().locals();
        assertEquals(8, locals.length);

        assertEquals(0, locals[0].length);
        assertEquals(0, locals[1].length);

        assertEquals(1, locals[2].length);
        assertEquals(1, locals[3].length);

        assertEquals("foo", locals[2][0].name());
        assertEquals(0, locals[2][0].offset());
        assertEquals("char*", locals[2][0].type());

        assertEquals(2, locals[4].length);
        assertEquals(2, locals[5].length);

        assertEquals("baz", locals[4][0].name());
        assertEquals(1, locals[4][0].offset());
        assertEquals("int", locals[4][0].type());
        assertEquals("foo", locals[4][1].name());
        assertEquals(0, locals[4][1].offset());
        assertEquals("char*", locals[4][1].type());

        assertEquals(1, locals[6].length);
        assertEquals(1, locals[7].length);

        assertEquals("baz", locals[6][0].name());
        assertEquals(1, locals[6][0].offset());
        assertEquals("int", locals[6][0].type());

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
    private static MonTanaMiniComputer assembleAndLoad(String assembly) {
        AssemblyResult result = assemble(assembly);
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data(), result.debugInfo());
        return computer;
    }

    @NotNull
    private static AssemblyResult assemble(String assembly) {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble(assembly);
        if (!result.errors().isEmpty()) {
            throw new RuntimeException("Assembly errors: \n\n" + result.printErrors());
        }
        return result;
    }
}
