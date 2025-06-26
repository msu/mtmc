package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

public class ErrorInstruction extends Instruction {
    public ErrorInstruction(List<MTMCToken> labels, MTMCToken instruction, String error) {
        super(null, labels, instruction);
        addError(instruction, error);
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        // do nothing
    }


}
