package edu.montana.cs.mtmc.asm.instructions;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

import java.util.HashMap;
import java.util.Map;

public class MiscInstruction extends Instruction {

    public static final Map<String, Integer> SYSCALLS;
    static {
        SYSCALLS = new HashMap<>();
        SYSCALLS.put("exit", 0x0000);
        SYSCALLS.put("rint", 0x0001);
        SYSCALLS.put("wint", 0x0002);
        SYSCALLS.put("rstr", 0x0003);
        SYSCALLS.put("wstr", 0x0004);
        SYSCALLS.put("rfile", 0x0005);
        SYSCALLS.put("wfile", 0x0006);
        SYSCALLS.put("rnd", 0x0007);
        SYSCALLS.put("sleep", 0x0008);
        SYSCALLS.put("fbreset", 0x0009);
        SYSCALLS.put("fbstat", 0x000A);
        SYSCALLS.put("fbset", 0x000B);
        SYSCALLS.put("error", 0x000F);
    }

    private MTMCToken syscallType;
    private MTMCToken fromRegister;
    private MTMCToken toRegister;

    public static boolean isSysCall(String str) {
        return SYSCALLS.containsKey(str);
    }
    public MiscInstruction(InstructionType type, MTMCToken label, MTMCToken instructionToken) {
        super(type, label, instructionToken);
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        if (getType() == InstructionType.SYS) {
            output[getLocation()] = 0b0000_0000;
            output[getLocation() + 1] = SYSCALLS.get(this.syscallType.getStringValue()).byteValue();
        } else if (getType() == InstructionType.MV) {
            int to = Registers.toInteger(toRegister.getStringValue());
            int from = Registers.toInteger(fromRegister.getStringValue());
            output[getLocation()] = 0b0000_0001;
            output[getLocation() + 1] = (byte) (to << 4 | from);
        } else if(getType() == InstructionType.NOOP) {
            output[getLocation()] = (byte) (0b0000_1111);
            output[getLocation() + 1] = (byte) (0b1111_1111);
        }
    }

    public void setSyscallType(MTMCToken type) {
        this.syscallType = type;
    }

    public void setFrom(MTMCToken fromRegister) {
        this.fromRegister = fromRegister;
    }

    public void setTo(MTMCToken toRegister) {
        this.toRegister = toRegister;
    }
}
