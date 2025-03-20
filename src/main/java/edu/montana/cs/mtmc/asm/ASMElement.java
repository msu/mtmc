package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.tokenizer.MTMCToken;

import java.util.ArrayList;
import java.util.List;

public abstract class ASMElement implements HasLocation {

    private final MTMCToken label;
    List<ASMError> errors = new ArrayList<>();
    private int location = -1;

    public ASMElement(MTMCToken label) {
        this.label = label;
    }

    public MTMCToken getLabel() {
        return label;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public List<ASMError> getErrors() {
        return errors;
    }

    public void addError(MTMCToken token, String error) {
        errors.add(new ASMError(token, error));
    }

}
