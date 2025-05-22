package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.tokenizer.MTMCToken;

public class ErrorInstruction extends Instruction {
    public ErrorInstruction(MTMCToken label, MTMCToken instruction, String error) {
        super(null, label, instruction);
        addError(instruction, error);
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        // do nothing
    }


}
