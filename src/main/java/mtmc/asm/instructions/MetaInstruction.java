package mtmc.asm.instructions;

import mtmc.asm.Assembler;
import mtmc.tokenizer.MTMCToken;

import java.util.List;

public class MetaInstruction extends Instruction {
    private MTMCToken originalFilePath;
    private MTMCToken originaLineNumber;
    private MTMCToken globalName;
    private MTMCToken globalLocation;
    private MTMCToken globalType;
    private MTMCToken localName;
    private MTMCToken localOffset;
    private MTMCToken localType;

    public MetaInstruction(MTMCToken instruction) {
        super(null, List.of(), instruction);
    }

    @Override
    public void genCode(byte[] output, Assembler assembler) {
        // do nothing
    }

    public boolean isFileDirective() {
        return "file".equals(this.getInstructionToken().stringValue());
    }

    public boolean isLineDirective() {
        return "line".equals(this.getInstructionToken().stringValue());
    }

    public boolean isGlobalDirective() {
        return "global".equals(this.getInstructionToken().stringValue());
    }

    public boolean isLocalDirective() {
        return "local".equals(this.getInstructionToken().stringValue());
    }

    public boolean isEndLocalDirective() {
        return "endlocal".equals(this.getInstructionToken().stringValue());
    }

    public void setOriginalFilePath(MTMCToken path) {
        this.originalFilePath = path;
    }

    public void setOriginalLineNumber(MTMCToken lineNumber) {
        this.originaLineNumber = lineNumber;
    }

    public int getOriginalLineNumber() {
        return this.originaLineNumber.intValue();
    }

    public void setGlobalInfo(MTMCToken name, MTMCToken location, MTMCToken type) {
        this.globalName = name;
        this.globalLocation = location;
        this.globalType = type;
    }

    public void setLocalInfo(MTMCToken name, MTMCToken offset, MTMCToken type) {
        this.localName = name;
        this.localOffset = offset;
        this.localType = type;
    }

    public void setEndLocalInfo(MTMCToken name) {
        this.localName = name;
    }

    public String getOriginalFilePath() {
        return this.originalFilePath.stringValue();
    }

    public String getGlobalName() {
        return this.globalName.stringValue();
    }

    public int getGlobalLocation() {
        return this.globalLocation.intValue();
    }

    public String getGlobalType() {
        return this.globalType.stringValue();
    }

    public String getLocalName() {
        return this.localName.stringValue();
    }

    public int getLocalOffset() {
        return this.localOffset.intValue();
    }

    public String getLocalType() {
        return this.localType.stringValue();
    }
}
