package mtmc.emulator;

import mtmc.asm.instructions.Instruction;
import mtmc.os.MTOS;
import mtmc.os.fs.FileSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static mtmc.emulator.MonTanaMiniComputer.ComputerStatus.*;
import static mtmc.emulator.Register.*;
import static mtmc.util.BinaryUtils.getBits;

public class MonTanaMiniComputer {

    // constants
    public static final short WORD_SIZE = 2;
    public static final int MEMORY_SIZE = 4096;

    // core model
    short[] registerFile; // 16 user visible + the instruction register
    byte[]  memory;
    byte[] breakpoints;
    private ComputerStatus status = READY;
    private int speed = 1000000;
    private MTMCIO io = new MTMCIO();

    // helpers
    MTOS os = new MTOS(this);
    MTMCConsole console = new MTMCConsole(this);
    MTMCDisplay display = new MTMCDisplay(this);
    MTMCClock clock = new MTMCClock(this);
    FileSystem fileSystem = new FileSystem(this);
    LinkedList<RewindStep> rewindSteps = new LinkedList<>();
    public static final int MAX_REWIND_STEPS = 100;

    // listeners
    private List<MTMCObserver> observers = new ArrayList<>();
    private DebugInfo debugInfo = null;
    private RewindStep currentRewindStep;

    public MonTanaMiniComputer() {
        initMemory();
    }

    public void initMemory() {
        registerFile = new short[Register.values().length];
        memory = new byte[MEMORY_SIZE];
        breakpoints = new byte[MEMORY_SIZE];
        rewindSteps = null;
        setRegisterValue(SP, (short) MEMORY_SIZE);  // default the stack pointer to the top of memory
        rewindSteps = new LinkedList<>();
        observers.forEach(MTMCObserver::computerReset);
    }

    public void load(byte[] code, byte[] data, DebugInfo debugInfo) {
        load(code, data, new byte[0][0], debugInfo);
    }
    
    public void load(byte[] code, byte[] data, byte[][] graphics, DebugInfo debugInfo) {

        this.debugInfo = debugInfo;
        graphics = (graphics == null ? new byte[0][0] : graphics);

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
        notifyOfStepExecution(); // prepare for step execution
        display.loadGraphics(graphics); // ready graphics for use

        // reset computer status
        setStatus(READY);
    }
    
    public long pulse(long instructions)
    {
        long count = 0;
        
        for (long i=0; i<instructions && status == EXECUTING; i++) {
            fetchAndExecute();
            count++;
            
            if(breakpoints[getRegisterValue(PC)] != 0) {
                setStatus(BREAK);
            }
        }
        
        return count;
    }

    public void run() {
        setStatus(EXECUTING);
        clock.run();
    }
    
    public void back() {
        clock.back();
    }

    public void step() {
        clock.step();
    }

    public void setStatus(ComputerStatus status) {
        this.status = status;
        this.notifyOfExecutionUpdate();
        
        if (status == FINISHED || status == BREAK) {
            this.notifyOfStepExecution();
        }
    }

    public ComputerStatus getStatus() {
        return status;
    }

    public void fetchAndExecute() {
        currentRewindStep = new RewindStep();
        rewindSteps.push(currentRewindStep);
        fetchCurrentInstruction();
        short instruction = getRegisterValue(IR);
        if (isDoubleWordInstruction(instruction)) {
            setRegisterValue(PC, (short) (getRegisterValue(PC) + 2 * WORD_SIZE));
        } else {
            setRegisterValue(PC, (short) (getRegisterValue(PC) + WORD_SIZE));
        }
        execInstruction(instruction);
    }

    public void execInstruction(short instruction) {
        observers.forEach(o -> o.beforeExecution(instruction));
        short instructionType = getBits(16, 4, instruction);
        if (instructionType == 0x0000) { // MISC
            short topNibble = getBits(12, 4, instruction);
            switch (topNibble) {
                case 0b0000 -> {
                    // sys call
                    short sysCall = getBits(8, 8, instruction);
                    os.handleSysCall(sysCall);
                }
                case 0b0001 -> {
                    // mov
                    short to = getBits(8, 4, instruction);
                    short from = getBits(4, 4, instruction);
                    short value = getRegisterValue(from);
                    setRegisterValue(to, value);
                }
                case 0b0010 -> {
                    // inc
                    short target = getBits(8, 4, instruction);
                    short immediateValue = getBits(4, 4, instruction);
                    short registerValue = getRegisterValue(target);
                    int value = registerValue + immediateValue;
                    setRegisterValue(target, value);
                }
                case 0b0011 -> {
                    // dec
                    short target = getBits(8, 4, instruction);
                    short immediateValue = getBits(4, 4, instruction);
                    short registerValue = getRegisterValue(target);
                    int value = registerValue - immediateValue;
                    setRegisterValue(target, value);
                }
                case 0b0100 -> {
                    // dec
                    short target = getBits(8, 4, instruction);
                    short immediateValue = getBits(4, 4, instruction);
                    setRegisterValue(target, immediateValue);
                }
               case 0b0101 -> {
                   // mcp
                   short source = getBits(8, 4, instruction);
                   source = getRegisterValue(source);
                   short dest = getBits(4, 4, instruction);
                   dest = getRegisterValue(dest);
                   int size = getRegisterValue(DR);
                   for (int i = 0; i < size; i++) {
                       byte value = fetchByteFromMemory(source + i);
                       writeByteToMemory(dest + i, value);
                   }
               }
                case 0b1000 -> {
                    // debug
                    short debugIndex = getBits(8, 8, instruction);
                    debugInfo.handleDebugString(debugIndex, this);
                }
                case 0b1111 -> {
                    // noop
                }
                default -> badInstruction(instruction);
            }
        } else if (instructionType == 0x0001) { // ALU
            short opCode = getBits(12, 4, instruction);

            short targetReg = getBits(8, 4, instruction);
            short sourceReg;

            if(opCode == 0b1111){
                // immediate, source is data register, opcode is the lowest nibble
                sourceReg = (short) Register.DR.ordinal();
                opCode = getBits(4, 4, instruction);
            } else if (opCode == 0b1100 || opCode == 0b1101 || opCode == 0b1110) {
                // unary
                sourceReg = targetReg;
            } else {
                // binary op, source is in the lowest nibble
                sourceReg = getBits(4, 4, instruction);
            }

            final short sourceValue = getRegisterValue(sourceReg);
            final short targetValue = getRegisterValue(targetReg);

            int result = 0;
            switch (opCode) {
                case 0b0000 -> {
                    result = targetValue + sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0001 -> {
                    // sub
                    result = targetValue - sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0010 -> {
                    // mul
                    result = targetValue * sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0011 -> {
                    // div
                    result = targetValue / sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0100 -> {
                    // mod
                    result = targetValue % sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0101 -> {
                    // and
                    result = targetValue & sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0110 -> {
                    // or
                    result = targetValue | sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b0111 -> {
                    // xor
                    result = targetValue ^ sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b1000 -> {
                    // shift left
                    result = targetValue << sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b1001 -> {
                    result = targetValue >>> sourceValue;
                    setRegisterValue(targetReg, result);
                }
                case 0b1010 -> {
                    result = Math.min(targetValue, sourceValue);
                    setRegisterValue(targetReg, result);
                }
                case 0b1011 -> {
                    result = Math.max(targetValue, sourceValue);
                    setRegisterValue(targetReg, result);
                }
                case 0b1100 -> {
                    result = ~targetValue;
                    setRegisterValue(targetReg, (short) result);
                }
                case 0b1101 -> {
                    result = targetValue == 0 ? 1 : 0;
                    setRegisterValue(targetReg, result);
                }
                case 0b1110 -> {
                    // negate
                    result = -targetValue;
                    setRegisterValue(targetReg, (short) result);
                }
                default -> badInstruction(instruction);
            }

            setFlagTestBit(result != 0);
        } else if (instructionType == 0b0010) {
            short opcode = getBits(12, 4, instruction);
            short stackReg = getBits(4, 4, instruction);
            switch (opcode) {
                case 0b0000 -> {
                    // push
                    short sourceRegister = getBits(8, 4, instruction);
                    // decrement the stack pointer
                    setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                    // write the value out to the location
                    short stackPointerValue = getRegisterValue(stackReg);
                    short valueToPush = getRegisterValue(sourceRegister);
                    writeWordToMemory(stackPointerValue, valueToPush);
                }
                case 0b0001 -> {
                    // pop
                    short targetRegister = getBits(8, 4, instruction);
                    short stackPointerValue = getRegisterValue(stackReg);
                    short value = fetchWordFromMemory(stackPointerValue);
                    setRegisterValue(targetRegister, value);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                }
                case 0b0010 -> {
                    // dup
                    short currentVal = fetchWordFromMemory(getRegisterValue(stackReg));
                    setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), currentVal);
                }
                case 0b0011 -> {
                    // swap
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), nextDown);
                    writeWordToMemory(getRegisterValue(stackReg) + WORD_SIZE, currentTop);
                }
                case 0b0100 -> // drop
                        setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                case 0b0101 -> {
                    // over
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                    // write the value out to the location
                    writeWordToMemory(getRegisterValue(stackReg), nextDown);
                }
                case 0b0110 -> {
                    // rot
                    short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                    short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                    short thirdDown = fetchWordFromMemory(getRegisterValue(stackReg) + 2 * WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), thirdDown);
                    writeWordToMemory(getRegisterValue(stackReg) + WORD_SIZE, currentTop);
                    writeWordToMemory(getRegisterValue(stackReg) + 2 * WORD_SIZE, nextDown);
                }
                case 0b0111 -> {
                    // sop
                    short aluOpCode = getBits(8, 4, instruction);
                    switch (aluOpCode) {
                        case 0b0000 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown + currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0001 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown - currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0010 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown * currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0011 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown / currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0100 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown % currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0101 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown & currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0110 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown | currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b0111 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown ^ currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b1000 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown << currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b1001 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int value = nextDown >>> currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b1010 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int min = Math.min(currentTop, nextDown);
                            writeWordToMemory(getRegisterValue(stackReg), min);
                        }
                        case 0b1011 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            short nextDown = fetchWordFromMemory(getRegisterValue(stackReg) + WORD_SIZE);
                            setRegisterValue(stackReg, getRegisterValue(stackReg) + WORD_SIZE);
                            int max = Math.max(currentTop, nextDown);
                            writeWordToMemory(getRegisterValue(stackReg), max);
                        }
                        case 0b1100 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            int value = ~currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b1101 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            int value = currentTop == 0 ? 1 : 0;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        case 0b1110 -> {
                            short currentTop = fetchWordFromMemory(getRegisterValue(stackReg));
                            int value = -currentTop;
                            writeWordToMemory(getRegisterValue(stackReg), value);
                        }
                        default -> badInstruction(instruction);
                    }
                }
                case 0b1111 -> {
                    short immediateValue = getRegisterValue(DR);
                    setRegisterValue(stackReg, getRegisterValue(stackReg) - WORD_SIZE);
                    writeWordToMemory(getRegisterValue(stackReg), immediateValue);
                }
                default -> badInstruction(instruction);
            }
        } else if (instructionType == 0b0011) {
            int opCode = getBits(12, 4, instruction);
            int lhs = getBits(8, 4, instruction);
            int rhs = getBits(4, 4, instruction);
            switch (opCode) {
                case 0b0000 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = getRegisterValue(rhs);
                    setFlagTestBit(lhsVal == rhsVal);
                }
                case 0b0001 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = getRegisterValue(rhs);
                    setFlagTestBit(lhsVal != rhsVal);
                }
                case 0b0010 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = getRegisterValue(rhs);
                    setFlagTestBit(lhsVal > rhsVal);
                }
                case 0b0011 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = getRegisterValue(rhs);
                    setFlagTestBit(lhsVal >= rhsVal);
                }
                case 0b0100 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = getRegisterValue(rhs);
                    setFlagTestBit(lhsVal < rhsVal);
                }
                case 0b0101 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = getRegisterValue(rhs);
                    setFlagTestBit(lhsVal <= rhsVal);
                }
                case 0b1000 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = (short) rhs;
                    setFlagTestBit(lhsVal == rhsVal);
                }
                case 0b1001 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = (short) rhs;
                    setFlagTestBit(lhsVal != rhsVal);
                }
                case 0b1010 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = (short) rhs;
                    setFlagTestBit(lhsVal > rhsVal);
                }
                case 0b1011 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = (short) rhs;
                    setFlagTestBit(lhsVal >= rhsVal);
                }
                case 0b1100 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = (short) rhs;
                    setFlagTestBit(lhsVal < rhsVal);
                }
                case 0b1101 -> {
                    short lhsVal = getRegisterValue(lhs);
                    short rhsVal = (short) rhs;
                    setFlagTestBit(lhsVal <= rhsVal);
                }
                default -> badInstruction(instruction);
            }
        } else if (0b1000 == instructionType) {
            // load/store
            int opCode = getBits(12, 4, instruction);
            int reg = getBits(8, 4, instruction);
            int offsetReg = getBits(4, 4, instruction);
            int address = getRegisterValue(DR);
            switch(opCode) {
                case 0b0000 -> {
                    short value = fetchWordFromMemory(address);
                    setRegisterValue(reg, value);
                }
                case 0b0001 -> {
                    short value = fetchWordFromMemory(address + getRegisterValue(offsetReg));
                    setRegisterValue(reg, value);
                }
                case 0b0010 -> {
                    short value = fetchByteFromMemory(address);
                    setRegisterValue(reg, value);
                }
                case 0b0011 -> {
                    short value = fetchByteFromMemory(address + getRegisterValue(offsetReg));
                    setRegisterValue(reg, value);
                }
                case 0b0100 -> {
                    short value = getRegisterValue(reg);
                    writeWordToMemory(address, value);
                }
                case 0b0101 -> {
                    short value = getRegisterValue(reg);
                    writeWordToMemory(address + getRegisterValue(offsetReg), value);
                }
                case 0b0110 -> {
                    byte value = (byte) getRegisterValue(reg);
                    writeByteToMemory(address, value);
                }
                case 0b0111 -> {
                    byte value = (byte) getRegisterValue(reg);
                    writeByteToMemory(address + getRegisterValue(offsetReg), value);
                }
                case 0b1111 -> {
                    setRegisterValue(reg, address);
                }
                default -> badInstruction(instruction);
            }
        } else if (0b0100 <= instructionType && instructionType <= 0b0111) {
            // load/store register
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
        } else if (0b1001 == instructionType) {
            // jump reg
            short reg = getBits(4, 4, instruction);
            short location = getRegisterValue(reg);
            setRegisterValue(PC, location);
        } else if (0b1100 <= instructionType && instructionType <= 0b1111) {
            // jumps
            short jumpType = getBits(14, 2, instruction);
            if(jumpType == 0b00) {
                // unconditional
                short location = getBits(12, 12, instruction);
                setRegisterValue(PC, location);
            } else if (jumpType == 0b01) {
                // jz
                short location = getBits(12, 12, instruction);
                if (!isFlagTestBitSet()) {
                    setRegisterValue(PC, location);
                }
            } else if (jumpType == 0b10) {
                // jnz
                short location = getBits(12, 12, instruction);
                if (isFlagTestBitSet()) {
                    setRegisterValue(PC, location);
                }
            } else if (jumpType == 0b11) {
                // jump & link
                short location = getBits(12, 12, instruction);
                setRegisterValue(RA, getRegisterValue(PC));
                setRegisterValue(PC, location);
            }
        } else {
            badInstruction(instruction);
        }
        observers.forEach(o -> o.afterExecution(instruction));
    }

    public boolean isFlagTestBitSet() {
        int i = getRegisterValue(FLAGS) & 0b0001;
        boolean b = i != 0;
        return b;
    }

    public void setFlagTestBit(boolean testVal) {
        short value = getRegisterValue(FLAGS);
        if (testVal) {
            value |= 0b0001;
        } else {
            value &= 0b1110;
        }
        setRegisterValue(FLAGS, value);
    }

    private void badInstruction(short instruction) {
        setStatus(PERMANENT_ERROR);
        // TODO implement flags
        console.println("BAD INSTRUCTION: 0x" + Integer.toHexString(instruction & 0xFFFF));
    }

    public void fetchCurrentInstruction() {
        short pc = getRegisterValue(PC);
        short instruction = fetchWordFromMemory(pc);
        setRegisterValue(IR, instruction);
        if (isDoubleWordInstruction(instruction)) {
            short data = fetchWordFromMemory(pc + WORD_SIZE);
            setRegisterValue(DR, data);
        } else {
            setRegisterValue(DR, 0);
        }
        observers.forEach(o -> o.instructionFetched(instruction));
    }

    public static boolean isDoubleWordInstruction(short instruction) {
        boolean isLoadStore = getBits(16, 4, instruction) == 0b1000;
        if (isLoadStore) {
            return true;
        }
        boolean isALUImmediate = getBits(16, 8, instruction) == 0b0001_1111;
        if (isALUImmediate) {
            return true;
        }
        boolean isPushImmediate = getBits(16, 8, instruction) == 0b0010_1111;
        if (isPushImmediate) {
            return true;
        }
        boolean isMcp = getBits(16, 8, instruction) == 0b0000_0101;
        if (isMcp) {
            return true;
        }
        return false;
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
        if (address < 0 || address >= memory.length) {
            setStatus(PERMANENT_ERROR);
            console.println("BAD MEMORY LOCATION ON READ: " + address + " (0x" + Integer.toHexString(address & 0xFFFF) + ")");
            return 0;
        } else {
            return memory[address];
        }
    }

    public void writeWordToMemory(int address, int value) {
        byte i = (byte) (value >>> 8);
        writeByteToMemory(address, i);
        writeByteToMemory(address + 1, (byte) (value & 0b11111111));
    }

    public void writeByteToMemory(int address, byte value) {
        if (address < 0 || address >= memory.length) {
            setStatus(PERMANENT_ERROR);
            console.println("BAD MEMORY LOCATION ON WRITE: " + address + " (0x" + Integer.toHexString(address & 0xFFFF) + ")");
            return;
        }
        byte currentValue = memory[address];
        addRewindStep(() -> memory[address] = currentValue);
        memory[address] = value;
        observers.forEach(o -> o.memoryUpdated(address, value));
    }

    private void addRewindStep(Runnable runnable) {
        if (currentRewindStep != null && rewindSteps != null) {
            currentRewindStep.addSubStep(runnable);
            if (rewindSteps.size() > MAX_REWIND_STEPS) {
                rewindSteps.removeLast();
            }
        }
    }

    public void setRegisterValue(Register register, int value) {
        setRegisterValue(register.ordinal(), value);
    }

    public void setRegisterValue(int register, int value) {
        if (Short.MAX_VALUE < value || value < Short.MIN_VALUE) {
            // TODO mark as overflow
        }
        short currentValue = registerFile[register];
        addRewindStep(() -> {
            registerFile[register] = currentValue;
        });
        registerFile[register] = (short) value;
        observers.forEach(o -> o.registerUpdated(register, value));
    }

    public short getRegisterValue(Register register) {
        return registerFile[register.ordinal()];
    }

    public short getRegisterValue(int register) {
        return registerFile[register];
    }
    
    public int[] getBreakpoints() {
        var list = new ArrayList<Integer>();
        
        for(int i=0; i<breakpoints.length; i++)
        {
            if(breakpoints[i] != 0) list.add(i);
        }
        
        var result = new int[list.size()];
        
        for(int i=0; i<result.length; i++) result[i] = list.get(i);
        
        return result;
    }
    
    public void setBreakpoint(int address, boolean active) {
        breakpoints[address] = (active ? (byte)1 : (byte)0);
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

    public void pause() {
        setStatus(READY);
    }

    public int getSpeed() {
        return speed;
    }
    
    public void setSpeed(int speed) {
        this.speed = speed;
        this.notifyOfExecutionUpdate();
    }

    public void notifyOfConsoleUpdate() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.consoleUpdated();
            }
        }
    }

    public void notifyOfConsolePrinting() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.consolePrinting();
            }
        }
    }

    public void notifyOfDisplayUpdate() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.displayUpdated();
            }
        }
    }

    public void notifyOfFileSystemUpdate() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.filesystemUpdated();
            }
        }
    }

    public void notifyOfExecutionUpdate() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.executionUpdated();
            }
        }
    }

    public void notifyOfStepExecution() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.stepExecution();
            }
        }
    }

    public void notifyOfRequestString() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.requestString();
            }
        }
    }

    public void notifyOfRequestCharacter() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.requestCharacter();
            }
        }
    }

    public void notifyOfRequestInteger() {
        if (observers != null) {
            for (MTMCObserver observer : observers) {
                observer.requestInteger();
            }
        }
    }
    
    public int getIOState() {
        return io.getValue();
    }

    public MTMCIO getIO() {
        return io;
    }

    public DebugInfo getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(DebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    public void setArg(String arg) {
        if (!arg.isEmpty()) {
            short start = getRegisterValue(BP);
            byte[] bytes = arg.getBytes();
            writeStringToMemory(start, bytes);
            setRegisterValue(A0, start);
            setRegisterValue(BP, start + bytes.length + 1);
        }
    }

    public void writeStringToMemory(int start, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte aByte = bytes[i];
            writeByteToMemory(start + i, aByte);
        }
        // null terminate
        writeByteToMemory(start + bytes.length, (byte) 0);
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void rewind() {
        RewindStep latestRewindStep = rewindSteps.pop();
        latestRewindStep.rewind();
    }

    public boolean isBackAvailable() {
        return !rewindSteps.isEmpty();
    }

    public enum ComputerStatus {
        READY,
        EXECUTING,
        PERMANENT_ERROR,
        FINISHED,
        WAITING,
        BREAK
    }

    public static void main(String[] args) {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        computer.setSpeed(1); // default to 1hz
        computer.start();
    }

    @Override
    public String toString() {
        var s = new StringBuilder();
        s.append("CI: ").append(Instruction.disassemble(getRegisterValue(IR), getRegisterValue(DR)));
        s.append(", Status: ").append(getStatus().name());
        var fp = getRegisterValue(FP);
        var sp = getRegisterValue(SP);
        s.append(", FP:SP=").append(fp).append(':').append(sp);
        s.append(", STACK: ");

        for (var i = fp; i > sp; i--) {
            byte[] value = getBytesFromMemory(i - 2, 2);
            int v = value[1] >>> 8 | value[0];
            s.append(v).append(' ');
        }

        return s.toString();
    }
}
