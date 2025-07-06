package mtmc.emulator;

import java.io.Console;

import static mtmc.emulator.MTMCConsole.Mode.*;
import mtmc.os.shell.Shell;
import mtmc.tokenizer.MTMCScanner;
import mtmc.tokenizer.MTMCToken;
import mtmc.tokenizer.MTMCTokenizer;

public class MTMCConsole {

    Mode mode = NON_INTERACTIVE;
    Console sysConsole = null;
    private final MonTanaMiniComputer computer;

    // non-interactive data
    private StringBuffer output = new StringBuffer();
    private short shortValue;
    private String stringValue;

    public MTMCConsole(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    // TODO invert so shell is driving and console is just IO
    public void start() {
        mode = INTERACTIVE;
        sysConsole = System.console();
        Shell.printShellWelcome(computer);
        while(true) {
            String cmd = sysConsole.readLine("mtmc > ");
            computer.getOS().processCommand(cmd);
        }
    }

    public void println(String x) {
        print(x);
        print("\n");
    }

    public void print(String x) {
        output.append(x);
        if(mode == INTERACTIVE) {
            System.out.print(x);
        } else {
            if (x.contains("\n")) { 
                computer.notifyOfConsoleUpdate();
            }
        }
    }

    public char readChar() {
        if (mode == INTERACTIVE) {
            var tokens = new MTMCScanner(sysConsole.readLine(), null).tokenize();
            var token = tokens.getFirst();
            assert token.type() == MTMCToken.TokenType.CHAR;
            return token.charValue();
        } else {
            return (char) this.shortValue;
        }
    }

    public short readInt() {
        if (mode == INTERACTIVE) {
            return Short.parseShort(sysConsole.readLine());
        } else {
            return shortValue;
        }
    }

    public void setShortValue(short shortValue) {
        this.shortValue = shortValue;
    }

    public void setCharValue(char charValue) {
        this.shortValue = (short) charValue;
    }

    public String getOutput() {
        return output.toString();
    }
    
    public String consumeLines() {
        int index = output.lastIndexOf("\n");
        String text = (index >= 0) ? output.substring(0, index+1) : "";
        
        if (index >= 0) {
            output.delete(0, index+1);
        }
        
        return text;
    }

    public void writeInt(short value) {
        print(value + "");
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String readString() {
        if(mode == INTERACTIVE) {
            return sysConsole.readLine();
        } else {
            return stringValue;
        }
    }

    public void resetOutput() {
        output.delete(0, output.length());
    }

    public enum Mode {
        NON_INTERACTIVE,
        INTERACTIVE,
    }
}
