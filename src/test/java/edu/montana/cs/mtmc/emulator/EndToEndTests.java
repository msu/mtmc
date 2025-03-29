package edu.montana.cs.mtmc.emulator;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.asm.AssemblyResult;
import org.junit.jupiter.api.Test;

import static edu.montana.cs.mtmc.emulator.Registers.*;
import static org.junit.jupiter.api.Assertions.*;

public class EndToEndTests {

    @Test
    public void addOneToOne(){
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
                mv t0 one
                add t0 t0
                """);
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data());
        computer.run();
        assertEquals(2, computer.getRegisterValue(T0));
    }

    @Test
    public void helloWorld(){
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
                .data
                hello_world: "hello world"
                .code
                ldi t0 hello_world
                mv a0 t0
                sys wstr
                """);
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data());
        computer.run();
        assertEquals("hello world", computer.getConsole().getOutput());
    }
}
