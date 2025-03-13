package edu.montana.cs.mtmc.emulator;

import edu.montana.cs.mtmc.os.MonTanaOperatingSystem;

import static edu.montana.cs.mtmc.emulator.ComputerStatus.*;
import static edu.montana.cs.mtmc.emulator.Registers.*;

public class MonTanaMiniComputer {

    // constants
    public static final short WORD_SIZE = 2;
    public static final int MEMORY_SIZE = 4096;
    public static final int FRAME_BUFF_START = MEMORY_SIZE / 2;

    // core model
    short[] registerFile = new short[17]; // 16 user visible + the instruction register
    byte[]  memory = new byte[MEMORY_SIZE];
    ComputerStatus status = READY;

    // helpers
    MonTanaOperatingSystem os = new MonTanaOperatingSystem(this);
    MTMCConsole console = new MTMCConsole(this);
    MTMCDisplay display = new MTMCDisplay(this);

    public MonTanaMiniComputer() {
        registerFile[SP] = FRAME_BUFF_START; // default the stack pointer to the top of normal memory
    }

    public void fetchAndExecute() {
        fetchInstruction();
        execInstruction(registerFile[IR]);
    }

    public void execInstruction(short instruction) {
        short instructionType = getBits(16, 4, instruction);
        if (instructionType == 0x0) {
            short specialInstructionType = getBits(12, 4, instruction);
            if(specialInstructionType == 0x0) {
                os.handleSysCall(getBits(8, 8, instruction));
            } else if(specialInstructionType == 0x1) {
                // move
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                registerFile[targetReg] = registerFile[sourceReg]; // move value
            }
        } else if (instructionType == 0x1) {
            short aluInstructionType = getBits(12, 4, instruction);
            if(aluInstructionType == 0x0) {
                // add
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                registerFile[targetReg] = (short) (registerFile[targetReg] + registerFile[sourceReg]);
            }
        } else if (instructionType == 0x2) {
            short stackInstructionType = getBits(12, 4, instruction);
            if(stackInstructionType == 0x0) {
                // push
                short sourceRegister = getBits(8, 4, instruction);
                short stackReg = getBits(4, 4, instruction);
                // decrement the stack pointer
                registerFile[stackReg] = (short) (registerFile[stackReg] - WORD_SIZE);
                // write the value out to the location
                writeWord(registerFile[stackReg], registerFile[sourceRegister]);
            }
        } else {
            status = PERMANENT_ERROR;
        }
    }

    public short getBits(int start, int totalBits, short instruction) {
        if (totalBits <= 0) {
            return 0;
        }
        int returnValue = instruction >> (start - totalBits);
        int mask = 0;
        while(totalBits > 0) {
            totalBits--;
            mask = mask << 1;
            mask = mask + 1;
        }
        return (short) (returnValue & mask);
    }

    private void fetchInstruction() {
        short pc = registerFile[PC];
        short instruction = fetchWord(pc);
        registerFile[IR] = instruction;
        registerFile[PC] = (short) (registerFile[PC] + WORD_SIZE);
    }

    public short fetchWord(int address) {
        short upperByte = fetchByte(address);
        byte lowerByte = fetchByte(address + 1);
        short instruction = (short) (upperByte << 8);
        instruction = (short) (instruction | lowerByte);
        return instruction;
    }

    public byte fetchByte(int address) {
        return memory[address];
    }

    public void writeWord(int address, short value) {
        byte i = (byte) (value >>> 8);
        writeByte(address, i);
        writeByte(address + 1, (byte) value);
    }

    public void writeByte(int address, byte value) {
        memory[address] = value;
    }

    public void setRegister(int register, int value) {
        registerFile[register] = (short) value;
    }

    public short getRegister(int register) {
        return registerFile[register];
    }

    private void start() {
        console.start();                     // start the interactive console
    }

    public static void main(String[] args) {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.start();
    }

}
