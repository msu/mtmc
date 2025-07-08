package mtmc.asm.data;

import mtmc.asm.ASMElement;
import mtmc.asm.Assembler;
import mtmc.tokenizer.MTMCToken;

import java.util.Arrays;
import java.util.List;

public class Data extends ASMElement {
    public MTMCToken valueToken;
    private byte[] value;

    public Data(List<MTMCToken> labels, int lineNumber) {
        super(labels, lineNumber);
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

    public void setValue(MTMCToken src, byte[] value) {
        this.valueToken = src;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Data{" +
                "value=" + Arrays.toString(value) +
                '}';
    }

    @Override
    public void addError(String err) {
        addError(getLabels().getLast(), err);
    }

}