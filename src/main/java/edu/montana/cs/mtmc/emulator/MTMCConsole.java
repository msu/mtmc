package edu.montana.cs.mtmc.emulator;

import edu.montana.cs.mtmc.web.WebServer;

import java.awt.*;
import java.io.Console;

import static edu.montana.cs.mtmc.emulator.MTMCConsole.Mode.INTERACTIVE;
import static edu.montana.cs.mtmc.emulator.MTMCConsole.Mode.NON_INTERACTIVE;

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

    public void start() {
        mode = INTERACTIVE;
        sysConsole = System.console();
        println("Welcome to the MTMC console!  Type ? for help, q to quit...");
        while(true) {
            try {
                String command = sysConsole.readLine("mtmc > ").strip();
                if (!command.isEmpty()) {
                    if (command.equals("q")) {
                        System.exit(1);
                    } else if(command.equals("web")) {
                        WebServer server = WebServer.getInstance(computer);
                        Desktop.getDesktop().browse(server.getURL());
                    } else {
                      println("Unknown command: " + command);
                    }
                }
            } catch (Exception e) {
                println("Error: " + e.getMessage());
            }
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
