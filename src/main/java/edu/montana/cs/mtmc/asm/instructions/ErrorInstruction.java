package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class ErrorInstruction extends Instruction {
    public ErrorInstruction(MTMCToken label, MTMCToken instruction, String error) {
        super(null, label, instruction);
        addError(instruction, error);
    }

    @Override
    public void genCode(short[] output) {
        // do nothing
    }
}
