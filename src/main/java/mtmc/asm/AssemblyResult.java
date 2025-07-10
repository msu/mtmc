package mtmc.asm;

import mtmc.emulator.DebugInfo;

import java.util.List;

public record AssemblyResult(byte[] code, byte[] data, byte[][] graphics, DebugInfo debugInfo, List<ASMError> errors) {
    public String printErrors() {
        StringBuilder builder = new StringBuilder("Errors:\n");
        for (ASMError error : errors) {
            builder.append("  Line " + error.token().line() + ": " + error.error()).append('\n');
        }
        return builder.toString();
    }
}
