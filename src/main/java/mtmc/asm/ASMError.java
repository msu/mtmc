package mtmc.asm;

import mtmc.tokenizer.MTMCToken;

public record ASMError(MTMCToken token, String error) {
    public String formattedErrorMessage() {
        return "Line " + token.line() + ": " + error;
    }
}
