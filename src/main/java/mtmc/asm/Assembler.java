package mtmc.asm;

import java.io.File;
import mtmc.asm.data.Data;
import mtmc.asm.instructions.*;
import mtmc.emulator.DebugInfo;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.SysCall;
import mtmc.os.exec.Executable;
import mtmc.tokenizer.MTMCToken;
import mtmc.tokenizer.MTMCTokenizer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import mtmc.asm.graphics.Graphic;

import static mtmc.asm.instructions.InstructionType.InstructionClass.*;
import static mtmc.tokenizer.MTMCToken.TokenType.*;

public class Assembler {

    List<Instruction> instructions = new ArrayList<>();
    int instructionsSize = 0;
    List<Data> data = new ArrayList<>();
    List<Graphic> graphics = new ArrayList<>();
    int dataSize = 0;
    HashMap<String, HasLocation> labels;

    ASMMode mode = ASMMode.TEXT;
    MTMCTokenizer tokenizer;
    private List<MTMCToken> lastLabels = List.of();
    private String srcName = "disk/file.asm";
    private List<String> debugStrings;

    public Assembler() {
        labels = new HashMap<>();
    }

    public AssemblyResult assemble(String asm) {
        return assemble(null, asm);
    }

    public AssemblyResult assemble(String file, String asm) {
        tokenizer = new MTMCTokenizer(asm, "#");
        debugStrings = new ArrayList<>();
        parseAssembly();
        resolveLocations();
        List<ASMError> errors = collectErrors();
        DebugInfo debugInfo = null;
        byte[] code = null, data = null;
        byte[][] graphics = null;
        if (errors.isEmpty()) {
            code = genCode();
            data = genData();
            graphics = genGraphics();
            debugInfo = genDebugInfo(file, asm, code);
        }
        return new AssemblyResult(code, data, graphics, debugInfo, errors);
    }

    private DebugInfo genDebugInfo(String assemblyFile, String assemblySource, byte[] code) {

        int[] assemblyLineNumbers = new int[code.length];

        String originalFile = "";
        int[] originalLineNumbers = new int[code.length];

        List<DebugInfo.GlobalInfo> globals = new ArrayList<>();
        DebugInfo.LocalInfo[][] locals = new DebugInfo.LocalInfo[code.length][];

        int location = 0;
        int originalLineNumber = 0;
        Map<String, DebugInfo.LocalInfo> currentLocals = new TreeMap<>();
        for (Instruction instruction : instructions) {
            int asmLineNumber = instruction.getLineNumber();
            if (instruction instanceof MetaInstruction mi) {
                if (mi.isFileDirective()) {
                    originalFile = mi.getOriginalFilePath();
                } else if (mi.isLineDirective()) {
                    originalLineNumber = mi.getOriginalLineNumber();
                } else if (mi.isGlobalDirective()) {
                    globals.add(new DebugInfo.GlobalInfo(mi.getGlobalName(), mi.getGlobalLocation(), mi.getGlobalType()));
                } else if (mi.isLocalDirective()) {
                    currentLocals.put(mi.getLocalName(), new DebugInfo.LocalInfo(mi.getLocalName(), mi.getLocalOffset(), mi.getLocalType()));
                } else if (mi.isEndLocalDirective()) {
                    currentLocals.remove(mi.getLocalName());
                }
            }
            while(location < instruction.getLocation() + instruction.getSizeInBytes()) {
                assemblyLineNumbers[location] = asmLineNumber;
                originalLineNumbers[location] = originalLineNumber;
                locals[location] = currentLocals.values().toArray(new DebugInfo.LocalInfo[0]);
                location++;
            }
        }

        DebugInfo debugInfo = new DebugInfo(debugStrings,
                assemblyFile,
                assemblySource,
                assemblyLineNumbers,
                originalFile,
                originalLineNumbers,
                globals.toArray(new DebugInfo.GlobalInfo[0]),
                locals);

        return debugInfo;
    }

    public Executable assembleExecutable(String srcName, String asm) {
        this.srcName = "disk/" + srcName; //TODO: The prefix should be replaced with FileSystem access
        
        var result = assemble(srcName, asm);
        if (!result.errors().isEmpty()) {
            throw new RuntimeException("Errors:\n" + result.errors()
                    .stream()
                    .map(e -> " - " + e.formattedErrorMessage())
                    .collect(Collectors.joining("\n")));
        }
        
        return new Executable(
                Executable.Format.Orc1,
                result.code(),
                result.data(),
                result.graphics(),
                srcName,
                result.debugInfo()
        );
    }

    private void parseAssembly() {
        while (tokenizer.more()) {
            parseLine();
        }
    }

    private byte[] genData() {
        byte[] dataBytes = new byte[dataSize];
        for (Data dataElt : data) {
            dataElt.genData(dataBytes, this);
        }
        return dataBytes;
    }

    private byte[] genCode() {
        byte[] code = new byte[instructionsSize];
        for (Instruction instruction : instructions) {
           instruction.genCode(code, this);
        }
        return code;
    }

    private int[] genOriginalLineNumbers(int length) {
        int[] asmLineNumbers = new int[length];
        int currentLineNumber = 0;
        int location = 0;
        for (Instruction instruction : instructions) {
            if (instruction instanceof MetaInstruction mi && mi.isLineDirective()) {
                currentLineNumber = mi.getOriginalLineNumber();
            }
            while(location < instruction.getLocation() + instruction.getSizeInBytes()) {
                asmLineNumbers[location] = currentLineNumber;
                location++;
            }
        }
        return asmLineNumbers;
    }

    private byte[][] genGraphics() {
        byte[][] graphics = new byte[this.graphics.size()][];
        int index = 0;
        for (Graphic graphic : this.graphics) {
            graphics[index++] = graphic.getImageData();
        }
        return graphics;
    }

    private List<ASMError> collectErrors() {
        List<ASMError> errors = new ArrayList<>();
        for (Instruction instruction : instructions) {
            errors.addAll(instruction.getErrors());
        }
        for (Data data : this.data) {
            errors.addAll(data.getErrors());
        }
        for (Graphic graphic : this.graphics) {
            errors.addAll(graphic.getErrors());
        }
        return errors;
    }

    private void resolveLocations() {
        // assign locations
        int offset = 0;
        for (Instruction instruction : instructions) {
            instruction.setLocation(offset);
            offset += instruction.getSizeInBytes();
            instructionsSize += instruction.getSizeInBytes();
        }
        for (Data data : data) {
            data.setLocation(offset);
            offset += data.getSizeInBytes();
            dataSize += data.getSizeInBytes();
        }
        for (Instruction instruction : instructions) {
            instruction.validateLabel(this);
        }
    }


    private void parseLine() {
        LinkedList<MTMCToken> tokens = getTokensForLine();

        if (parseMetaDirective(tokens)) {
            return;
        }

        ASMMode newMode = parseModeFlag(tokens);
        if (newMode != null) {
            mode = newMode;
            return;
        }
        // label only lines are ok and propagate to the next instruction
        List<MTMCToken> labelTokens = maybeGetLabels(tokens);
        var labels = new ArrayList<>(lastLabels);
        labels.addAll(labelTokens);
        lastLabels = labels;
        if (tokens.isEmpty()) {
            for (MTMCToken labelToken : labelTokens) {
                Data labelData = new Data(labelTokens, labelToken.line());
                if (hasLabel(labelToken.stringValue())) {
                    labelData.addError(tokens.poll(), "Label already defined: " + labelToken.stringValue());
                } else {
                    this.labels.put(labelToken.labelValue(), labelData);
                }
                data.add(labelData);
            }
        } else if (mode == ASMMode.TEXT) {
            parseInstruction(tokens, labels);
        } else {
            parseData(tokens, labels);
        }
    }

    private boolean parseMetaDirective(LinkedList<MTMCToken> tokens) {
        if (tokens.size() >= 2 && tokens.get(0).type() == AT) {
            tokens.removeFirst();
            MetaInstruction metaInstruction = new MetaInstruction(tokens.removeFirst());
            if (metaInstruction.isFileDirective()) {
                MTMCToken path = requireString(tokens, metaInstruction);
                metaInstruction.setOriginalFilePath(path);
            } else if (metaInstruction.isGlobalDirective()) {
                MTMCToken name = requireString(tokens, metaInstruction);
                MTMCToken location = requireIntegerToken(tokens, metaInstruction, Integer.MAX_VALUE);
                MTMCToken type = requireString(tokens, metaInstruction);
                metaInstruction.setGlobalInfo(name, location, type);
            } else if (metaInstruction.isLocalDirective()) {
                MTMCToken name = requireString(tokens, metaInstruction);
                MTMCToken offset = requireIntegerToken(tokens, metaInstruction, Integer.MAX_VALUE);
                MTMCToken type = requireString(tokens, metaInstruction);
                metaInstruction.setLocalInfo(name, offset, type);
            } else if (metaInstruction.isEndLocalDirective()) {
                MTMCToken name = requireString(tokens, metaInstruction);
                metaInstruction.setEndLocalInfo(name);
            } else if (metaInstruction.isLineDirective()) {
                MTMCToken lineNumber = requireIntegerToken(tokens, metaInstruction, Integer.MAX_VALUE);
                metaInstruction.setOriginalLineNumber(lineNumber);
            } else {
                metaInstruction.addError("Unknown meta directive");
            }
            instructions.add(metaInstruction);
            return true;
        }
        return false;
    }

    private ASMMode parseModeFlag(LinkedList<MTMCToken> tokens) {
        if (tokens.size() >= 2
                && tokens.get(0).type() == DOT
                && tokens.get(1).type() == IDENTIFIER
                && tokens.get(0).end() == tokens.get(1).start()
                && (tokens.get(1).stringValue().equals("data") ||
                tokens.get(1).stringValue().equals("text"))) {
            return ASMMode.valueOf(tokens.get(1).stringValue().toUpperCase());
        }
        return null;
    }

    private void parseData(LinkedList<MTMCToken> tokens, List<MTMCToken> labelTokens) {
        lastLabels = List.of();
        MTMCToken dataToken = tokens.poll();
        Data dataElt = new Data(labelTokens, dataToken == null ? 0 : dataToken.line());
        if(dataToken != null) {
            if (dataToken.type() == STRING) {
                byte[] stringBytes = dataToken.stringValue().getBytes(StandardCharsets.US_ASCII);
                byte[] nullTerminated = new byte[stringBytes.length + 1];
                System.arraycopy(stringBytes, 0, nullTerminated, 0, stringBytes.length);
                nullTerminated[stringBytes.length] = '\0';
                dataElt.setValue(dataToken, nullTerminated);
            } else if (isInteger(dataToken)) {
                int integerValue = dataToken.intValue();
                if (integerValue > Short.MAX_VALUE || integerValue < Short.MIN_VALUE) {
                    dataElt.addError(dataToken, "Number is too large");
                }
                dataElt.setValue(dataToken, new byte[]{(byte) (integerValue >>> 8), (byte) integerValue});
            } else if (dataToken.type() == DOT) {
                dataToken = tokens.poll();
                dataElt = new Data(labelTokens, dataToken.line());
                if (dataToken.stringValue().equals("int")) {
                    MTMCToken intToken = requireIntegerToken(tokens, dataElt, MonTanaMiniComputer.MEMORY_SIZE);
                    if (intToken != null) {
                        dataElt.setValue(intToken, new byte[intToken.intValue() * 2]);
                    }
                } else if (dataToken.stringValue().equals("byte")) {
                    MTMCToken intToken = requireIntegerToken(tokens, dataElt, MonTanaMiniComputer.MEMORY_SIZE);
                    if (intToken != null) {
                        dataElt.setValue(intToken, new byte[intToken.intValue()]);
                    }
                } else if (dataToken.stringValue().equals("image")) {
                    MTMCToken stringToken = requireString(tokens, dataElt);
                    if (stringToken != null) {
                        loadGraphic(labelTokens, dataElt, stringToken);
                    }
                } else {
                    dataElt.addError(dataToken, "only data types are .int, .byte, and .image");
                }
            } else if (dataToken.type() == MINUS) {
                MTMCToken nextToken = tokens.poll(); // get next
                if (nextToken == null || (nextToken.type() != INTEGER && nextToken.type() != HEX && nextToken.type() != BINARY)) {
                    dataElt.addError(dataToken, "Number is too negative");
                } else {
                    int integerValue = -1 * nextToken.intValue();
                    if (integerValue < Short.MIN_VALUE) {
                        dataElt.addError(dataToken, "Number is too negative");
                    }
                    var joinToken = MTMCToken.join(dataToken, nextToken, INTEGER);
                    dataElt.setValue(joinToken, new byte[]{(byte) (integerValue >>> 8), (byte) integerValue});
                }
            } else {
                dataElt.addError(dataToken, "Unknown token type: " + dataToken.toString());
            }
        }

        for (MTMCToken labelToken : labelTokens) {
            if (hasLabel(labelToken.stringValue())) {
                dataElt.addError(tokens.poll(), "Label already defined: " + labelToken.stringValue());
            } else {
                labels.put(labelToken.labelValue(), dataElt);
            }
        }
        data.add(dataElt);
    }
    
    private Graphic loadGraphic(List<MTMCToken> labelTokens, Data data, MTMCToken token) {
        Graphic graphic = new Graphic(labelTokens, token.line());
        String filename = token.stringValue();
        File file = new File(new File(this.srcName).getParent(), filename);
        int index = graphics.size();
        
        data.setValue(token, new byte[]{ (byte)((index >> 8) & 0xFF), (byte)(index & 0xFF) });
        graphic.setImage(file.getPath());
        graphics.add(graphic);
        
        return graphic;
    }

    private void parseInstruction(LinkedList<MTMCToken> tokens, List<MTMCToken> labelTokens) {
        MTMCToken instructionToken = tokens.peekFirst();
        if (instructionToken == null) return;

        lastLabels = List.of();
        if (instructionToken.type() != IDENTIFIER) {
            instructions.add(new ErrorInstruction(labelTokens, instructionToken, "Invalid Token"));
            return;
        }

        tokens = transformSyntheticInstructions(tokens);

        instructionToken = tokens.poll();
        InstructionType type = InstructionType.fromString(instructionToken.stringValue());

        if (type == null) {
            instructions.add(new ErrorInstruction(labelTokens, instructionToken, "Unknown instruction token type: " + instructionToken.stringValue()));
            return;
        }

        Instruction instruction;
        if (type.getInstructionClass() == MISC) {
            MiscInstruction miscInst = new MiscInstruction(type, labelTokens, instructionToken);
            if (type == InstructionType.SYS) {
                MTMCToken sysCallType = requireSysCall(tokens, miscInst);
                miscInst.setSyscallType(sysCallType);
            } else if (type == InstructionType.MOV) {
                MTMCToken toRegister = requireWriteableRegister(tokens, miscInst);
                miscInst.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, miscInst);
                miscInst.setFrom(fromRegister);
            } else if (type == InstructionType.INC || type == InstructionType.DEC) {
                MTMCToken toRegister = requireWriteableRegister(tokens, miscInst);
                miscInst.setTo(toRegister);
                if (!tokens.isEmpty()) {
                    MTMCToken value = requireIntegerToken(tokens, miscInst, 15);
                    miscInst.setValue(value);
                }
            } else if (type == InstructionType.SETI) {
                MTMCToken toRegister = requireWriteableRegister(tokens, miscInst);
                miscInst.setTo(toRegister);
                MTMCToken value = requireIntegerToken(tokens, miscInst, 15);
                miscInst.setValue(value);
            } else if (type == InstructionType.MCP) {
                MTMCToken fromRegister = requireWriteableRegister(tokens, miscInst);
                miscInst.setFrom(fromRegister);
                MTMCToken toRegister = requireWriteableRegister(tokens, miscInst);
                miscInst.setTo(toRegister);
                MTMCToken value = requireIntegerToken(tokens, miscInst, Short.MAX_VALUE);
                miscInst.setValue(value);
            } else if (type == InstructionType.DEBUG) {
                MTMCToken debugString = requireString(tokens, miscInst);
                // create a dummy int token representing the offset of the debug string in the debug info
                int debugStringIndex = debugStrings.size();
                debugStrings.add(debugString.stringValue());
                MTMCToken value = new MTMCToken(0, 0, 0, 0, String.valueOf(debugStringIndex), INTEGER);
                miscInst.setValue(value);
            }
            instruction = miscInst;
        } else if (type.getInstructionClass() == ALU) {
            ALUInstruction aluInst = new ALUInstruction(type, labelTokens, instructionToken);
            if (aluInst.isBinaryOp()) {
                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, aluInst);
                aluInst.setFrom(fromRegister);
            } else if (aluInst.isImmediateOp()) {
                MTMCToken immediateOp = requireALUOp(tokens, aluInst);
                // TODO - validate is max or lower op
                aluInst.setImmediateOp(immediateOp);

                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);

                MTMCToken value = requireIntegerToken(tokens, aluInst, Short.MAX_VALUE);
                aluInst.setImmediateValue(value);
            } else {
                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);
            }
            instruction = aluInst;
        } else if (type.getInstructionClass() == STACK) {
            StackInstruction stackInst = new StackInstruction(type, labelTokens, instructionToken);
            if (type == InstructionType.PUSH) {
                MTMCToken targetRegister = requireReadableRegister(tokens, stackInst);
                stackInst.setTarget(targetRegister);
            } else if (type == InstructionType.POP) {
                MTMCToken toRegister = requireWriteableRegister(tokens, stackInst);
                stackInst.setTarget(toRegister);
            } else if (type == InstructionType.SOP) {
                MTMCToken aluOp = requireAluOp(tokens, stackInst);
                stackInst.setALUOp(aluOp);
            } else if (type == InstructionType.PUSHI) {
                MTMCToken value = requireIntegerToken(tokens, stackInst, Short.MAX_VALUE);
                stackInst.setValue(value);
            }

            // if there is a stack register specified, consume it
            if (!tokens.isEmpty() && tokens.peekFirst().type() == IDENTIFIER) {
                MTMCToken stackReg = requireReadableRegister(tokens, stackInst);
                stackInst.setStackRegister(stackReg);
            }

            instruction = stackInst;
        } else if (type.getInstructionClass() == TEST) {
            TestInstruction testInst = new TestInstruction(type, labelTokens, instructionToken);
            testInst.setFirst(requireReadableRegister(tokens, testInst));
            if(testInst.isImmediate()) {
                testInst.setImmediateValue(requireIntegerToken(tokens, testInst, 15));
            } else {
                testInst.setSecond(requireReadableRegister(tokens, testInst));
            }
            instruction = testInst;
        } else if (type.getInstructionClass() == LOAD_STORE) {
            LoadStoreInstruction loadInst = new LoadStoreInstruction(type, labelTokens, instructionToken);

            MTMCToken targetReg = requireReadableRegister(tokens, loadInst);
            loadInst.setTargetToken(targetReg);

            if (loadInst.isOffset()) {
                MTMCToken offsetReg = requireReadableRegister(tokens, loadInst);
                loadInst.setOffsetToken(offsetReg);
            }

            MTMCToken value = requireIntegerOrLabelReferenceToken(tokens, loadInst);
            loadInst.setValue(value);

            instruction = loadInst;
        } else if (type.getInstructionClass() == LOAD_STORE_REGISTER) {
            LoadStoreRegisterInstruction loadInst = new LoadStoreRegisterInstruction(type, labelTokens, instructionToken);

            MTMCToken targetReg = requireWriteableRegister(tokens, loadInst);
            loadInst.setTargetToken(targetReg);

            MTMCToken pointerReg = requireWriteableRegister(tokens, loadInst);
            loadInst.setPointerToken(pointerReg);

            // if there is an offset register specified, consume it
            if (!tokens.isEmpty() && tokens.peekFirst().type() == IDENTIFIER) {
                MTMCToken offsetReg = requireReadableRegister(tokens, loadInst);
                loadInst.setOffsetToken(offsetReg);
            }
            instruction = loadInst;
        } else if (type.getInstructionClass() == JUMP_REGISTER) {
            JumpRegisterInstruction jumpInst = new JumpRegisterInstruction(type, labelTokens, instructionToken);
            MTMCToken register = requireReadableRegister(tokens, jumpInst);
            jumpInst.setRegister(register);
            instruction = jumpInst;
        } else if (type.getInstructionClass() == JUMP) {
            JumpInstruction jumpInst = new JumpInstruction(type, labelTokens, instructionToken);
            MTMCToken labelValue = maybeGetLabelReference(tokens);
            MTMCToken valueToken;
            if (labelValue != null) {
                valueToken = labelValue;
            } else {
                valueToken = requireIntegerToken(tokens, jumpInst, MonTanaMiniComputer.MEMORY_SIZE);
            }
            jumpInst.setAddressToken(valueToken);
            instruction = jumpInst;
        } else {
            instruction = new ErrorInstruction(null, instructionToken, "Unexpected Token");
        }

        for (MTMCToken labelToken : labelTokens) {
            if (hasLabel(labelToken.stringValue())) {
                instruction.addError(tokens.poll(), "Label already defined: " + labelToken.stringValue());
            } else {
                labels.put(labelToken.labelValue(), instruction);
            }
        }

        // add error if any tokens are left on the line
        if (!tokens.isEmpty()) {
            instruction.addError(tokens.poll(), "Unexpected Token");
        }

        instructions.add(instruction);
    }

    public static LinkedList<MTMCToken> transformSyntheticInstructions(LinkedList<MTMCToken> tokens) {
        if (!tokens.isEmpty()) {
            MTMCToken first = tokens.peekFirst();
            if (first.type() == IDENTIFIER) {
                String stringVal = first.stringValue();
                if (stringVal.endsWith("i")) {
                    String op = stringVal.substring(0, stringVal.length() - 1);
                    if (ALUOp.isALUOp(op)) {
                        MTMCToken syntheticImmediate = tokens.removeFirst();
                        tokens.addFirst(syntheticImmediate.cloneWithVal(op));
                        tokens.addFirst(syntheticImmediate.cloneWithVal("imm"));
                    }
                } else if (stringVal.startsWith("s")) {
                    String op = stringVal.substring(1, stringVal.length());
                    if (ALUOp.isALUOp(op)) {
                        MTMCToken syntheticImmediate = tokens.removeFirst();
                        tokens.addFirst(syntheticImmediate.cloneWithVal(op));
                        tokens.addFirst(syntheticImmediate.cloneWithVal("sop"));
                    }
                } else if (stringVal.equals("la")) {
                    MTMCToken syntheticImmediate = tokens.removeFirst();
                    tokens.addFirst(syntheticImmediate.cloneWithVal("li"));
                } else if (stringVal.equals("ret")) {
                    MTMCToken syntheticImmediate = tokens.removeFirst();
                    tokens.addFirst(syntheticImmediate.cloneWithVal("ra"));
                    tokens.addFirst(syntheticImmediate.cloneWithVal("jr"));
                }
            }
        }
        return tokens;
    }

    //===================================================
    // tokenization helper functions
    //===================================================
    private MTMCToken requireSysCall(LinkedList<MTMCToken> tokens, Instruction inst) {
        MTMCToken sysCallType = tokens.poll();
        if (sysCallType == null) {
            inst.addError("Syscall required");
        } else if (sysCallType.type() != IDENTIFIER) {
            inst.addError(sysCallType, "Syscall required");
        } else if (!SysCall.isSysCall(sysCallType.stringValue())) {
            inst.addError(sysCallType, "Unknown syscall : " + sysCallType.stringValue());
        }
        return sysCallType;
    }

    private MTMCToken requireAluOp(LinkedList<MTMCToken> tokens, Instruction inst) {
        MTMCToken sysCallType = tokens.poll();
        if (sysCallType == null) {
            inst.addError("Syscall required");
        } else if (sysCallType.type() != IDENTIFIER) {
            inst.addError(sysCallType, "Syscall required");
        } else if (!ALUOp.isALUOp(sysCallType.stringValue())) {
            inst.addError(sysCallType, "Unknown alu operation : " + sysCallType.stringValue());
        }
        return sysCallType;
    }

    private static List<MTMCToken> maybeGetLabels(LinkedList<MTMCToken> tokens) {
        LinkedList<MTMCToken> labels = new LinkedList<>();
        while (!tokens.isEmpty() &&  tokens.getFirst().type() == LABEL) {
            MTMCToken label = tokens.poll();
            labels.add(label);
        }
        return labels;
    }

    private static MTMCToken maybeGetLabelReference(LinkedList<MTMCToken> tokens) {
        MTMCToken label = null;
        if (tokens.getFirst().type() == IDENTIFIER) {
            label = tokens.poll();
        }
        return label;
    }

    private MTMCToken requireWriteableRegister(LinkedList<MTMCToken> tokens, Instruction instruction) {
        MTMCToken nextToken = tokens.poll();
        if (nextToken == null) {
            instruction.addError("Register required");
        } else if (nextToken.type() != IDENTIFIER) {
            instruction.addError(nextToken, "Invalid Register : " + nextToken.stringValue());
        } else if (!Register.isWriteable(nextToken.stringValue())) {
            instruction.addError(nextToken, "Register not writeable : " + nextToken.stringValue());
        }
        return nextToken;
    }

    private MTMCToken requireReadableRegister(LinkedList<MTMCToken> tokens, Instruction instruction) {
        MTMCToken nextToken = tokens.poll();
        if (nextToken == null) {
            instruction.addError("Register required");
        } else if (nextToken.type() != IDENTIFIER) {
            instruction.addError(nextToken, "Invalid Register : " + nextToken.stringValue());
        } else if (!Register.isReadable(nextToken.stringValue())) {
            instruction.addError(nextToken, "Register not readable : " + nextToken.stringValue());
        }
        return nextToken;
    }

    private MTMCToken requireALUOp(LinkedList<MTMCToken> tokens, Instruction instruction) {
        MTMCToken nextToken = tokens.poll();
        if (nextToken == null || nextToken.type() != IDENTIFIER || !ALUOp.isALUOp(nextToken.stringValue())) {
            instruction.addError("ALU operation required");
        }
        return nextToken;
    }

    private MTMCToken requireString(LinkedList<MTMCToken> tokens, ASMElement instruction) {
        MTMCToken nextToken = tokens.poll();
        if (nextToken == null || nextToken.type() != STRING) {
            instruction.addError("String required");
        }
        return nextToken;
    }

    private MTMCToken requireIntegerToken(LinkedList<MTMCToken> tokens,
                                          ASMElement inst,
                                          int max) {
        MTMCToken token = tokens.poll();
        if (token == null) {
            inst.addError( "Integer value required");
        } else if (isInteger(token)) {
            Integer integerValue = token.intValue();
            if (integerValue < 0 || max < integerValue) {
                inst.addError(token, "Integer value out of range: 0-" + max);
            }
        } else {
            inst.addError(token, "Integer value expected");
        }
        return token;
    }

    private MTMCToken requireToken(LinkedList<MTMCToken> tokens, MTMCToken.TokenType type, ASMElement inst) {
        MTMCToken token = tokens.poll();
        if (token == null || token.type() != type) {
            inst.addError(token, "Token "  + type.name() + " required");
        }
        return token;
    }

    private MTMCToken requireIntegerOrLabelReferenceToken(LinkedList<MTMCToken> tokens, LoadStoreInstruction inst) {
        MTMCToken token = tokens.poll();
        if (token == null) {
            inst.addError("Integer or label value required");
        } else if (!isInteger(token) && token.type() != IDENTIFIER) {
            inst.addError(token, "Integer or label value expected");
        }
        return token;
    }



    private static boolean isInteger(MTMCToken token) {
        return token != null && (token.type() == INTEGER || token.type() == HEX || token.type() == BINARY);
    }

    private LinkedList<MTMCToken> getTokensForLine() {
        LinkedList<MTMCToken> tokens = new LinkedList<>();
        if (tokenizer.more()) {
            MTMCToken first = tokenizer.consume();
            tokens.add(first);
            while (tokenizer.more() &&
                    first.line() == tokenizer.currentToken().line()) {
                tokens.add(tokenizer.consume());
            }
        }
        return tokens;
    }

    public boolean hasLabel(String label) {
        return labels.containsKey(label);
    }

    public int resolveLabel(String label) {
        return labels.get(label).getLocation();
    }

    public int getInstructionsSizeInBytes() {
        return instructionsSize;
    }

    enum ASMMode {
        DATA,
        TEXT
    }

}
