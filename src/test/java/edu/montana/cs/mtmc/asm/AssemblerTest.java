package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.asm.Assembler.AssemblyResult;
import edu.montana.cs.mtmc.asm.instructions.MiscInstruction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AssemblerTest {

    @Test
    public void bootstrapAssembly() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("sys exit");
        assertArrayEquals(new short[]{0b0000_0000_0000_0000}, result.code());
    }

    @Test
    public void sysCalls() {
        Map<String, Integer> syscalls = MiscInstruction.SYSCALLS;
        for (String sysCall : syscalls.keySet()) {
            Assembler assembler = new Assembler();
            Integer value = syscalls.get(sysCall);
            AssemblyResult result = assembler.assemble("sys " + sysCall);
            assertArrayEquals(new short[]{value.shortValue()}, result.code());
        }
    }

    @Test
    public void move() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("mv t0 t1");
        assertArrayEquals(new short[]{0b0000_0001_0000_0001}, result.code());
    }

    @Test
    public void badMove() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("mv");
        assertNull(result.code());
        assertEquals(result.errors().size(), 2);
    }

    @Test
    public void badMoveRegister() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("mv foo bar");
        assertNull(result.code());
        assertEquals(result.errors().size(), 2);
    }

    @Test
    public void badMoveToUnwriteableRegister() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("mv zero t0");
        assertNull(result.code());
        assertEquals(result.errors().size(), 1);
    }

    @Test
    public void badMoveFromUnreadableRegister() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("mv t0 ir");
        assertNull(result.code());
        assertEquals(result.errors().size(), 1);
    }
}
