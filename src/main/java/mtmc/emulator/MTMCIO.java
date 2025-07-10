package mtmc.emulator;

public class MTMCIO {

    int value = 0;

    enum Buttons{
        UP(0b1000_0000),
        DOWN(0b0100_0000),
        LEFT(0b0010_0000),
        RIGHT(0b0001_0000),
        START(0b0000_1000),
        SELECT(0b0000_0100),
        B(0b0000_0010),
        A(0b0000_0001);

        private final int mask;
        Buttons(int mask) {
            this.mask = mask;
        }
    }

    public void keyPressed(String key) {
        Buttons button = Buttons.valueOf(key.toUpperCase());
        value = value | button.mask;
    }

    public void keyReleased(String key) {
        Buttons button = Buttons.valueOf(key.toUpperCase());
        value = value & ~button.mask;
    }

    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }

}
