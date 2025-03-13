package edu.montana.cs.mtmc.emulator;

import edu.montana.cs.mtmc.web.WebServer;

import java.awt.*;
import java.io.Console;
import java.net.URI;

public class MTMCConsole {

    private final MonTanaMiniComputer computer;

    public MTMCConsole(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public void start() {
        Console console = System.console();
        print("Welcome to the MTMC console!  Type ? for help, q to quit...");
        while(true) {
            try {
                String command = console.readLine("mtmc > ").strip();
                if (!command.isEmpty()) {
                    if (command.equals("q")) {
                        System.exit(1);
                    } else if(command.equals("web")) {
                        WebServer server = WebServer.getInstance(computer);
                        Desktop.getDesktop().browse(server.getURL());
                    } else {
                      print("Unknown command: " + command);
                    }
                }
            } catch (Exception e) {
                print("Error: " + e.getMessage());
            }
        }
    }

    private static void print(String x) {
        System.out.println(x);
    }

    private static void printerr(String x) {
        System.err.println(x);
    }
}
