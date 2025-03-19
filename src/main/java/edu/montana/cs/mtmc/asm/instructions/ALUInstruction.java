package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ALUInstruction extends Instruction {

    private static final Map<String, Integer> ALU_OPS;
    static {
        ALU_OPS = new HashMap<>();
        ALU_OPS.put("add", 0x0000);
        ALU_OPS.put("sub", 0x0001);
        ALU_OPS.put("mul", 0x0002);
        ALU_OPS.put("div", 0x0003);
        ALU_OPS.put("mod", 0x0004);
        ALU_OPS.put("and", 0x0005);
        ALU_OPS.put("or", 0x0006);
        ALU_OPS.put("xor", 0x0007);
        ALU_OPS.put("shl", 0x0008);
        ALU_OPS.put("shr", 0x0009);
        ALU_OPS.put("eq", 0x000A);
        ALU_OPS.put("lt", 0x000B);
        ALU_OPS.put("lteq", 0x000C);
        ALU_OPS.put("bnot", 0x000D);
        ALU_OPS.put("not", 0x000E);
        ALU_OPS.put("neg", 0x000F);
    }
    private static final List<String> UNARY_OPS = List.of("bnot", "not", "neg");
    public static boolean isALUInstruction(String instruction) {
        return ALU_OPS.containsKey(instruction);
    }
    public static int getALUOpcode(String instruction) {
        return ALU_OPS.get(instruction);
    }

    public ALUInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    private MTMCToken toToken;
    private MTMCToken fromToken;

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        int opCode = getALUOpcode(getInstructionToken().getStringValue());
        int to = Registers.toInteger(toToken.getStringValue());
        int from = 0;
        if (fromToken != null) {
            from = Registers.toInteger(fromToken.getStringValue());
        }
        output[getLocation()] = (byte) (0b0001_0000 | opCode);
        output[getLocation() + 1] = (byte) (to << 4 | from);
    }

    public void setTo(MTMCToken to) {
        this.toToken =  to;
    }

    public void setFrom(MTMCToken from) {
        this.fromToken = from;
    }

    public boolean isBinary() {
        return !UNARY_OPS.contains(getInstructionToken().getStringValue());
    }
}
