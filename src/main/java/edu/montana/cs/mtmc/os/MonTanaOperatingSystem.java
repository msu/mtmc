package edu.montana.cs.mtmc.os;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;

public class MonTanaOperatingSystem {

    private final MonTanaMiniComputer computer;

    public MonTanaOperatingSystem(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public void handleSysCall(short syscallNumber) {

    }
}
