package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.os.SysCall;
import edu.montana.cs.mtmc.util.BinaryUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssemblerTest {

    @Test
    public void bootstrapAssembly() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("sys halt");
        assertArrayEquals(new byte[]{0b0000_0000, 0b0000_0000}, result.code());
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
    public void move() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("mv t0 t1");
        assertArrayEquals(new byte[]{0b0000_0001, 0b0000_0001}, result.code());
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

    @Test
    public void aluOp() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        sub t1 t2
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0001_0001, 0b001_0010);
    }

    @Test
    public void stackOperationNoStackReg() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        sop add
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0010_0011, 0b0000_1011);
    }

    @Test
    public void stackOperationWithStackReg() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        sop sub t3
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0010_0011, 0b0001_0011);
    }

    @Test
    public void stackImmediateOperationNoStackReg() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        pushi 15
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0011_1011, 0b0000_1111);
    }

    @Test
    public void stackImmediateOperationWithStackReg() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        pushi 15 t3
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0011_0011, 0b0000_1111);
    }

    @Test
    public void loadStoreInstructionNoOffset() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        lb t2 t3
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0101_0010, 0b0011_1110);
    }

    @Test
    public void loadStoreInstructionWithOffset() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        lb t2 t3 t1
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b0101_0010, 0b0011_0001);
    }

    @Test
    public void loadImmediateInstructionNoOffset() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        ldi t2 15
        """);
        assertBytesAtOffsetAre(result.code(), 0, 0b1010_0000, 0b0000_1111);
    }

    @Test
    public void numericJump() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        noop     # offset start by 1 word 
        noop     # dummy inst
        j 2      # should be unconditional jump to location 2
        """);
        assertBytesAtOffsetAre(result.code(), 4, 0b1100_0000, 0b0000_0010);
    }

    @Test
    public void labeledJump() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
        noop         # offset start by 1 word 
        start: noop  # dummy inst
        j start      # should be unconditional jump to location 2
        """);
        assertBytesAtOffsetAre(result.code(), 4, 0b1100_0000, 0b0000_0010);
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


}
