package edu.montana.cs.mtmc.asm;

import java.util.ArrayList;
import java.util.List;

import static edu.montana.cs.mtmc.asm.Assembler.ASMMode.*;

public class Assembler {

    List<Instruction> instructions = new ArrayList<>();
    List<Data> data = new ArrayList<>();

    public void assemble(String asm) {
        String[] lines = asm.split("\n");
        ASMMode mode = CODE;
        for (String line : lines) {
        }
    }

    enum ASMMode {
        DATA,
        CODE
    }

}
