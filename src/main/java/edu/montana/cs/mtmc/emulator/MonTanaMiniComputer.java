package edu.montana.cs.mtmc.emulator;

import edu.montana.cs.mtmc.os.MonTanaOperatingSystem;

import java.io.IOException;

import static edu.montana.cs.mtmc.emulator.Registers.*;

public class MonTanaMiniComputer {

    short[] registerFile = new short[17];
    byte[]  memory = new byte[4096];
    MonTanaOperatingSystem os = new MonTanaOperatingSystem();

    public void fetchAndExecute() {
        fetchInstruction();
        execInstruction(registerFile[IR]);
    }

    public void execInstruction(short instruction) {

    }

    public short getBits(int start, int end, short instruction) {
        if (start < end) {
            return 0;
        }
        int returnValue = instruction >> end;
        int mask = 0b1;
        while(start > 0) {
            start--;
            mask = mask << 1;
            mask = mask + 1;
        }
        return (short) (returnValue & mask);
    }

    private void fetchInstruction() {
        short pc = registerFile[PC];
        short instruction = fetchWord(pc);
        registerFile[IR] = instruction;
        registerFile[PC]++;
    }

    private short fetchWord(int address) {
        short upperBytes = fetchByte(address);
        byte lowerBytes = fetchByte(address + 1);
        short instruction = (short) (upperBytes << 8);
        instruction &= lowerBytes;
        return instruction;
    }

    private byte fetchByte(int address) {
        return memory[address];
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new java.io.File(".").getCanonicalPath());
    }

}
