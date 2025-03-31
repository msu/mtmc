package mtmc.asm;

import mtmc.tokenizer.MTMCToken;

public record ASMError(MTMCToken token, String error) {
}
