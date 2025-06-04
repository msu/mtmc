package mtmc.emulator;

import mtmc.asm.Assembler;
import mtmc.asm.AssemblyResult;
import org.junit.jupiter.api.Test;

import static mtmc.emulator.Register.*;
import static org.junit.jupiter.api.Assertions.*;

public class EndToEndTests {

    @Test
    public void addOneToOne(){
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
                li t0 1
                add t0 t0
                """);
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data(), result.debugInfo());
        computer.run();
        assertEquals(2, computer.getRegisterValue(T0));
    }

    @Test
    public void stackAddition(){
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
                pushi 10
                pushi 99
                sop add
                pushi 99
                sop add
                pop t0
                """);
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data(), result.debugInfo());
        computer.run();
        assertEquals(208, computer.getRegisterValue(T0));
    }

    @Test
    public void helloWorld(){
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
                .data
                  hello_world: "hello world"
                .text
                  li a0 hello_world
                  sys wstr
                """);
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data(), result.debugInfo());
        computer.run();
        assertEquals("hello world", computer.getConsole().getOutput());
    }

    @Test
    public void negativeImmediates() {
        Assembler assembler = new Assembler();
        AssemblyResult result = assembler.assemble("""
                .data
                  value: -1
                .text
                  lw a0 value
                  sys wint
                  sys exit
                """);
        if (!result.errors().isEmpty()) {
            fail(result.printErrors());
        }

        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.load(result.code(), result.data(), result.debugInfo());
        computer.run();
        var output = computer.getConsole().getOutput();
        System.out.println(output);
    }
}
