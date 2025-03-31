package mtmc.emulator;

import mtmc.os.MTOS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static mtmc.emulator.MonTanaMiniComputer.ComputerStatus.*;
import static mtmc.emulator.Register.*;
import static mtmc.util.BinaryUtils.getBits;

public class MonTanaMiniComputer {

    // constants
    public static final short WORD_SIZE = 2;
    public static final int MEMORY_SIZE = 4096;
    public static final int FRAME_BUFF_START = MEMORY_SIZE * 3 / 4;

    // core model
    short[] registerFile; // 16 user visible + the instruction register
    byte[]  memory;
    ComputerStatus status = READY;

    // helpers
    MTOS os = new MTOS(this);
    MTMCConsole console = new MTMCConsole(this);
    MTMCDisplay display = new MTMCDisplay(this);

    // listeners
    private List<MTMCObserver> observers = new ArrayList<>();
    private int speed = 0;

    public MonTanaMiniComputer() {
        initMemory();
    }

    public void initMemory() {
        registerFile = new short[19];
        memory = new byte[MEMORY_SIZE];
        setRegisterValue(SP, (short) FRAME_BUFF_START);  // default the stack pointer to the top of normal memory
        setRegisterValue(ZERO, 0);
        setRegisterValue(ONE,  1);
        observers.forEach(MTMCObserver::computerReset);
    }

    public void load(byte[] code, byte[] data) {
        // reset memory
        initMemory();

        int codeBoundary = code.length;
        System.arraycopy(code, 0, memory, 0, codeBoundary);
        setRegisterValue(CB, codeBoundary - 1);

        int dataBoundary = codeBoundary + data.length;
        System.arraycopy(data, 0, memory, codeBoundary, data.length);
        setRegisterValue(DB, dataBoundary - 1);

        // base pointer starts just past the end of the data boundary
        setRegisterValue(BP, dataBoundary);

        fetchCurrentInstruction(); // fetch the initial instruction for display purposes

        // reset computer status
        status = READY;
    }

    public void run() {
        status = EXECUTING;
        while (status == EXECUTING) {
            long startTime = 0;
            if (speed > 0) {
                startTime = System.currentTimeMillis();
            }
            fetchAndExecute();
            if (speed > 0) {
                int delay = 1000 / speed;
                long endTime = System.currentTimeMillis();
                long instructionTime = endTime - startTime;
                long delta = delay - instructionTime;
                if (delta > 0) {
                    try {
                        Thread.sleep(delta);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void setStatus(ComputerStatus status) {
        this.status = status;
    }

    public ComputerStatus getStatus() {
        return status;
    }

    public void fetchAndExecute() {
        fetchCurrentInstruction();
        short instruction = getRegisterValue(IR);
        setRegisterValue(PC, (short) (getRegisterValue(PC) + WORD_SIZE));
        execInstruction(instruction);
    }

    public void execInstruction(short instruction) {
        observers.forEach(o -> o.beforeExecution(instruction));
        short instructionType = getBits(16, 4, instruction);
        if (instructionType == 0x0) {
            short specialInstructionType = getBits(12, 4, instruction);
            if(specialInstructionType == 0x0) {
                os.handleSysCall(getBits(8, 8, instruction));
            } else if(specialInstructionType == 0x1) {
                // move
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                short value = getRegisterValue(sourceReg);
                setRegisterValue(targetReg, value);
            } else if(specialInstructionType == 0xF && getBits(8, 8, instruction) == 0xFF) {
                // no op
            } else {
                // todo error state?
            }
        } else if (instructionType == 0x1) {
            // alu
            short aluInstructionType = getBits(12, 4, instruction);
            if(aluInstructionType == 0x0) {
                // add
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) + getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x1) {
                // sub
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) - getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x2) {
                // mul
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) * getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x3) {
                // div
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) / getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x4) {
                // mod
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) % getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x5) {
                // and
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) & getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x6) {
                // or
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) | getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x7) {
                // xor
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) ^ getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x8) {
                // shift left
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) << getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0x9) {
                // shift right
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) >>> getRegisterValue(sourceReg));
            } else if(aluInstructionType == 0xA) {
                // eq
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) == getRegisterValue(sourceReg) ? 1 : 0);
            } else if(aluInstructionType == 0xB) {
                // lt
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) < getRegisterValue(sourceReg) ? 1 : 0);
            } else if(aluInstructionType == 0xC) {
                // lte
                short targetReg = getBits(8, 4, instruction);
                short sourceReg = getBits(4, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) <= getRegisterValue(sourceReg) ? 1 : 0);
            } else if(aluInstructionType == 0xD) {
                // bitwise not
                short targetReg = getBits(8, 4, instruction);
                setRegisterValue(targetReg, (short) ~getRegisterValue(targetReg));
            } else if(aluInstructionType == 0xE) {
                // logical not
                short targetReg = getBits(8, 4, instruction);
                setRegisterValue(targetReg, getRegisterValue(targetReg) == 0 ? 1 : 0);
            } else if(aluInstructionType == 0xF) {
                // negate
                short targetReg = getBits(8, 4, instruction);
                setRegisterValue(targetReg, (short) -getRegisterValue(targetReg));
            }
        } else if (instructionType == 0x2) {
            short stackInstructionType = getBits(12, 4, instruction);
            short stackReg = getBits(4, 4, instruction);
            if(stackInstructionType == 0x0) {
                // push
                short sourceRegister = getBits(8, 4, instruction);
                // decrement the stack pointer
                setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                // write the value out to the location
                writeWordToMemory(getRegisterValue(stackReg), getRegisterValue(sourceRegister));
            } if(stackInstructionType == 0x1) {
                // pop
                short targetRegister = getBits(8, 4, instruction);
                // save the value into the location
                setRegisterValue(targetRegister, fetchWordFromMemory(getRegisterValue(stackReg)));
                // increment the stack pointer
                setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
            } if(stackInstructionType == 0x2) {
                short stackOpSubType = getBits(8, 4, instruction);
                if(stackOpSubType == 0x0) {
                    // dup
                    short currentVal = fetchWordFromMemory(getRegisterValue(stackReg));
                    setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), currentVal);
                } else if(stackOpSubType == 0x1) {
                    // swap
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown);
                    writeWordToMemory(getRegisterValue(stackReg) + WORD_SIZE, currentTop);
                } else if(stackOpSubType == 0x2) {
                    // drop
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                } else if(stackOpSubType == 0x3) {
                    // over
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                    // write the value out to the location
                    writeWordToMemory(getRegisterValue(stackReg), nextDown);
                } else if(stackOpSubType == 0x4) {
                    // rot
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    short thirdDown = fetchWordFromMemory(getRegisterValue(stackReg) + 2 * WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), thirdDown);
                    writeWordToMemory(getRegisterValue(stackReg) + WORD_SIZE, currentTop);
                    writeWordToMemory(getRegisterValue(stackReg) + 2 * WORD_SIZE, nextDown);
                } else {
                    // TODO error state
                }
            } if(stackInstructionType == 0x3) {
                short aluOp = getBits(8, 4, instruction);
                if (aluOp == 0x0) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown + currentTop);
                } else if (aluOp == 0x1) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown - currentTop);
                } else if (aluOp == 0x2) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown * currentTop);
                } else if (aluOp == 0x3) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown / currentTop);
                } else if (aluOp == 0x4) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown % currentTop);
                } else if (aluOp == 0x5) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown & currentTop);
                } else if (aluOp == 0x6) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown | currentTop);
                } else if (aluOp == 0x7) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown ^ currentTop);
                } else if (aluOp == 0x8) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown << currentTop);
                } else if (aluOp == 0x9) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown >>> currentTop);
                } else if (aluOp == 0xA) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown == currentTop ? 1 : 0);
                } else if (aluOp == 0xB) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown < currentTop ? 1 : 0);
                } else if (aluOp == 0xC) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown <= currentTop ? 1 : 0);
                } else if (aluOp == 0xD) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    writeWordToMemory(getRegisterValue(stackReg), ~currentTop);
                } else if (aluOp == 0xE) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    writeWordToMemory(getRegisterValue(stackReg), currentTop == 0 ? 1 : 0);
                } else if (aluOp == 0xF) {
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    writeWordToMemory(getRegisterValue(stackReg), -currentTop);
                }
            } else {
                // todo error state
            }
        } else if (instructionType == 0x3) {
            // pushi
            short stackReg = getBits(12, 4, instruction);
            short value = getBits(8, 8, instruction);
            setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
            writeWordToMemory(getRegisterValue(stackReg), value);
        } else if (0x4 <= instructionType && instructionType <= 0x7) {
            // load store
            short loadStoreType = getBits(14, 2, instruction);
            short targetRegister = getBits(12, 4, instruction);
            short addressRegister = getBits(8, 4, instruction);
            short offsetRegister = getBits(4, 4, instruction);
            int targetAddress = getRegisterValue(addressRegister) + getRegisterValue(offsetRegister);
            if(loadStoreType == 0x0) {
                setRegisterValue(targetRegister, fetchWordFromMemory(targetAddress));
            } else if (loadStoreType == 0x1) {
                setRegisterValue(targetRegister, fetchByteFromMemory(targetAddress));
            } else if (loadStoreType == 0x2) {
                writeWordToMemory(targetAddress, getRegisterValue(targetRegister));
            } else if (loadStoreType == 0x3) {
                writeByteToMemory(targetAddress, (byte) getRegisterValue(targetRegister));
            }
        } else if (0x8 <= instructionType && instructionType <= 0xB) {
            // load immediate
            short targetRegister = getBits(14, 2, instruction);
            short value = getBits(12, 12, instruction);
            setRegisterValue(targetRegister, value);
        } else if (0xC <= instructionType && instructionType <= 0xF) {
            short jumpType = getBits(14, 2, instruction);
            short location = getBits(12, 12, instruction);
            if(jumpType == 0x0) {
                setRegisterValue(PC, location);
            } else if (jumpType == 0x1) {
                if (getRegisterValue(T0) == 0) {
                    setRegisterValue(PC, location);
                }
            } else if (jumpType == 0x2) {
                if (getRegisterValue(T0) != 0) {
                    setRegisterValue(PC, location);
                }
            } else if (jumpType == 0x3) {
                setRegisterValue(RA, getRegisterValue(PC));
                setRegisterValue(PC, location);
            }
        } else {
            status = PERMANENT_ERROR;
        }
        observers.forEach(o -> o.afterExecution(instruction));
    }

    public void fetchCurrentInstruction() {
        short pc = getRegisterValue(PC);
        short instruction = fetchWordFromMemory(pc);
        setRegisterValue(IR, instruction);
        observers.forEach(o -> o.instructionFetched(instruction));
    }

    public short fetchWordFromMemory(int address) {
        short upperByte = fetchByteFromMemory(address);
        byte lowerByte = fetchByteFromMemory(address + 1);
        short value = (short) (upperByte << 8);
        int i = value | Byte.toUnsignedInt(lowerByte);
        value = (short) i;
        return value;
    }

    public byte fetchByteFromMemory(int address) {
        return memory[address];
    }

    public void writeWordToMemory(int address, int value) {
        byte i = (byte) (value >>> 8);
        writeByteToMemory(address, i);
        writeByteToMemory(address + 1, (byte) (value & 0b11111111));
    }

    public void writeByteToMemory(int address, byte value) {
        memory[address] = value;
        observers.forEach(o -> o.memoryUpdated(address, value));
        if (address >= FRAME_BUFF_START) {
            observers.forEach(o -> o.displayUpdated(address, value));
        }
    }

    public void setRegisterValue(Register register, int value) {
        setRegisterValue(register.ordinal(), value);
    }

    public void setRegisterValue(int register, int value) {
        if (Short.MAX_VALUE < value || value < Short.MIN_VALUE) {
            // TODO mark as overflow
        }
        registerFile[register] = (short) value;
        observers.forEach(o -> o.registerUpdated(register, value));
    }

    public short getRegisterValue(Register register) {
        return registerFile[register.ordinal()];
    }

    public short getRegisterValue(int register) {
        return registerFile[register];
    }

    private void start() {
        console.start();                     // start the interactive console
    }

    public MTMCConsole getConsole() {
        return console;
    }

    public byte[] getBytesFromMemory(int address, int length) {
        return Arrays.copyOfRange(memory, address, address + length);
    }

    public byte[] getMemory() {
        return memory;
    }

    public MTOS getOS() {
        return os;
    }

    public MTMCDisplay getDisplay() {
        return display;
    }

    public Iterable<Integer> getMemoryAddresses() {
        return () -> IntStream.range(0, MEMORY_SIZE).iterator();
    }

    public void addObserver(MTMCObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MTMCObserver observer) {
        observers.remove(observer);
    }

    public void resetFrameBuffer() {
        for (int i = MonTanaMiniComputer.FRAME_BUFF_START; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    public void pause() {
        status = READY;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public enum ComputerStatus {
        READY,
        EXECUTING,
        PERMANENT_ERROR,
        HALTED,
        WAITING
    }

    public static void main(String[] args) {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setSpeed(1); // default to 1hz
        computer.start();
    }
}
