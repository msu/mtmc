package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public record ASMError(MTMCToken token, String error) {
}
