package mtmc.asm;

import java.util.List;

public record AssemblyResult(byte[] code, byte[] data, List<ASMError> errors, String source) {
    public String printErrors() {
        StringBuilder builder = new StringBuilder("Errors:");
        for (ASMError error : errors) {
            builder.append("  Line " + error.token().line() + ": " + error.error());
        }
        return builder.toString();
    }
}
