package edu.montana.cs.mtmc.emulator;

import java.util.stream.IntStream;

public class MTMCDisplay {

    private final MonTanaMiniComputer computer;

    public MTMCDisplay(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    public Iterable<Integer> getRows() {
        return () -> IntStream.range(0, 64).iterator();
    }

    public Iterable<Integer> getColumns() {
        return () -> IntStream.range(0, 64).iterator();
    }

    public String getColorFor(int row, int column) {
        return "#2a453b";
    }
}
