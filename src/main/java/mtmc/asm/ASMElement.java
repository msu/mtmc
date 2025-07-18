package mtmc.asm;

import mtmc.tokenizer.MTMCToken;

import java.util.ArrayList;
import java.util.List;

public abstract class ASMElement implements HasLocation {

    private final List<MTMCToken> labels;
    List<ASMError> errors = new ArrayList<>();
    private int location = -1;
    protected int lineNumber;

    public ASMElement(List<MTMCToken> labels, int lineNumber) {
        this.labels = labels;
        this.lineNumber = lineNumber;
    }

    public List<MTMCToken> getLabels() {
        return labels;
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

    abstract public void addError(String integerValueRequired);

    public int getLineNumber() {
        return lineNumber;
    }
}
