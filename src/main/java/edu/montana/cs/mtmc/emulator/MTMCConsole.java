package edu.montana.cs.mtmc.emulator;

import java.io.Console;

import static edu.montana.cs.mtmc.emulator.MTMCConsole.Mode.*;
import edu.montana.cs.mtmc.os.shell.Shell;

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

    public String getOutput() {
        return output.toString();
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

    public enum Mode {
        NON_INTERACTIVE,
        INTERACTIVE,
    }
}
