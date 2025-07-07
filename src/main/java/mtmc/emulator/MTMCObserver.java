package mtmc.emulator;

public interface MTMCObserver {

    void consoleUpdated();

    void executionUpdated();
    
    void filesystemUpdated();
    
    void registerUpdated(int register, int value);

    void memoryUpdated(int address, byte value);

    void displayUpdated();

    void instructionFetched(short instruction);

    void beforeExecution(short instruction);

    void afterExecution(short instruction);

    void computerReset();
    
    void requestCharacter();
    
    void requestInteger();
    
    void requestString();
}
