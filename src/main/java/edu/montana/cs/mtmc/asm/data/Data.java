package edu.montana.cs.mtmc.asm.data;

import edu.montana.cs.mtmc.asm.ASMElement;
import edu.montana.cs.mtmc.asm.Assembler;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;

public class Data extends ASMElement {

    private byte[] value;

    public Data(MTMCToken label) {
        super(label);
    }

    @Override
    public int getSizeInBytes() {
        if (value == null) {
            return 0;
        } else {
            return value.length;
        }
    }

    public void genData(byte[] dataBytes, Assembler assembler) {
        int offset = getLocation() - assembler.getInstructionsSizeInBytes();
        for (int i = 0; i < getSizeInBytes(); i++) {
            byte dataByte = value[i];
            dataBytes[offset + i] = dataByte;
        }
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}