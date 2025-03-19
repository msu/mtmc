package edu.montana.cs.mtmc.asm.data;

import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.asm.HasLocation;

public class Data implements HasLocation {
    private String label;
    private int location = -1;
    private byte[] value;

    public Data(byte[] value) {
        this.value = value;
    }

    @Override
    public int getLocation() {
        return location;
    }

    @Override
    public int getSizeInBytes() {
        return 0;
    }

    public void setLocation(int offset) {
        location = offset;
    }

    public void genCode(byte[] dataBytes, Assembler assembler) {

    }
}