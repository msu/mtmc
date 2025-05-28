package mtmc.asm;

import mtmc.emulator.DebugInfo;

import java.util.List;

public record AssemblyResult(byte[] code, byte[] data, DebugInfo debugInfo, List<ASMError> errors, String source) {
    public String printErrors() {
        StringBuilder builder = new StringBuilder("Errors:");
        for (ASMError error : errors) {
            builder.append("  Line " + error.token().line() + ": " + error.error());
        }
        return builder.toString();
    }
}
