package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.asm.instructions.Instruction;

import java.util.List;

public record AssemblyResult(byte[] code, byte[] data, List<Instruction.Error> errors, String source) {
    public String printErrors() {
        StringBuilder builder = new StringBuilder("Errors:");
        for (Instruction.Error error : errors) {
            builder.append("  Line " + error.token().getLine() + ": " + error.error());
        }
        return builder.toString();
    }
}
