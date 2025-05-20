package mtmc.asm;

import mtmc.asm.data.Data;
import mtmc.asm.instructions.*;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.SysCall;
import mtmc.os.exec.Executable;
import mtmc.tokenizer.MTMCToken;
import mtmc.tokenizer.MTMCTokenizer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static mtmc.asm.instructions.InstructionType.InstructionClass.*;
import static mtmc.tokenizer.MTMCToken.TokenType.*;

public class Assembler {

    List<Instruction> instructions = new ArrayList<>();
    int instructionsSize = 0;
    List<Data> data = new ArrayList<>();
    int dataSize = 0;
    HashMap<String, HasLocation> labels;

    ASMMode mode = ASMMode.TEXT;
    MTMCTokenizer tokenizer;
    private MTMCToken lastLabel;

    public Assembler() {
        labels = new HashMap<>();
        labels.put(FrameBufferLocation.FRAME_BUFFER, new FrameBufferLocation());
    }

    public AssemblyResult assemble(String asm) {
        tokenizer = new MTMCTokenizer(asm, "#");
        parseAssembly();
        resolveLocations();
        List<ASMError> errors = collectErrors();
        byte[] code = null, data = null;
        if (errors.isEmpty()) {
            code = codeGen();
            data = dataGen();
        }
        return new AssemblyResult(code, data, errors, asm);
    }

    public Executable assembleExecutable(String srcName, String asm) {
        var result = assemble(asm);
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
                srcName
        );
    }

    private void parseAssembly() {
        while (tokenizer.more()) {
            parseLine();
        }
    }

    private byte[] dataGen() {
        byte[] dataBytes = new byte[dataSize];
        for (Data dataElt : data) {
            dataElt.genData(dataBytes, this);
        }
        return dataBytes;
    }

    private byte[] codeGen() {
        byte[] code = new byte[instructionsSize];
        for (Instruction instruction : instructions) {
            instruction.genCode(code, this);
        }
        return code;
    }

    private List<ASMError> collectErrors() {
        List<ASMError> errors = new ArrayList<>();
        for (Instruction instruction : instructions) {
            errors.addAll(instruction.getErrors());
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
        ASMMode newMode = parseModeFlag(tokens);
        if (newMode != null) {
            mode = newMode;
            return;
        }
        // label only lines are ok and propagate to the next instruction
        MTMCToken labelToken = maybeGetLabel(tokens);
        if (labelToken != null) {
            if (tokens.isEmpty()) {
                lastLabel = labelToken;
            } else {
                lastLabel = null; // labels always reset after one line
            }
        } else {
            labelToken = lastLabel;
            lastLabel = null; // labels always reset after one line
        }
        if (mode == ASMMode.TEXT) {
            parseInstruction(tokens, labelToken);
        } else {
            parseData(tokens, labelToken);
        }
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

    private void parseData(LinkedList<MTMCToken> tokens, MTMCToken labelToken) {
        MTMCToken dataToken = tokens.poll();
        Data dataElt = new Data(labelToken);
        if (dataToken == null) {
            dataElt.addError(labelToken, "Expected data");
        } else {
            if (dataToken.type() == STRING) {
                byte[] stringBytes = dataToken.stringValue().getBytes(StandardCharsets.US_ASCII);
                byte[] nullTerminated = new byte[stringBytes.length + 1];
                System.arraycopy(stringBytes, 0, nullTerminated, 0, stringBytes.length);
                nullTerminated[stringBytes.length] = '\0';
                dataElt.setValue(dataToken, nullTerminated);
            } else if (isInteger(dataToken)) {
                int integerValue = dataToken.intValue();
                if (integerValue > Short.MAX_VALUE) {
                    dataElt.addError(dataToken, "Number is too large");
                }
                dataElt.setValue(dataToken, new byte[]{(byte) (integerValue >>> 8), (byte) integerValue});
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
                dataElt.addError(dataToken, "Unknown token type");
            }
        }

        if (labelToken != null) {
            if (hasLabel(labelToken.stringValue())) {
                dataElt.addError(tokens.poll(), "Label already defined: " + labelToken.stringValue());
            } else {
                labels.put(labelToken.labelValue(), dataElt);
            }
        }
        data.add(dataElt);
    }

    private void parseInstruction(LinkedList<MTMCToken> tokens, MTMCToken labelToken) {
        MTMCToken instructionToken = tokens.peekFirst();
        if (instructionToken == null) return;

        if (instructionToken.type() != IDENTIFIER) {
            instructions.add(new ErrorInstruction(labelToken, instructionToken, "Invalid Token"));
            return;
        }

        tokens = handleSyntheticInstructions(tokens);

        instructionToken = tokens.poll();
        InstructionType type = InstructionType.fromString(instructionToken.stringValue());

        if (type == null) {
            instructions.add(new ErrorInstruction(labelToken, instructionToken, "Unknown instruction token type: " + instructionToken.stringValue()));
            return;
        }

        Instruction instruction;
        if (type.getInstructionClass() == MISC) {
            MiscInstruction miscInst = new MiscInstruction(type, labelToken, instructionToken);
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
            }
            instruction = miscInst;
        } else if (type.getInstructionClass() == ALU) {
            ALUInstruction aluInst = new ALUInstruction(type, labelToken, instructionToken);
            if (aluInst.isBinaryOp()) {
                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, aluInst);
                aluInst.setFrom(fromRegister);
            } else if (aluInst.isImmediatOp()) {
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
            StackInstruction stackInst = new StackInstruction(type, labelToken, instructionToken);
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
            TestInstruction testInst = new TestInstruction(type, labelToken, instructionToken);
            testInst.setFirst(requireReadableRegister(tokens, testInst));
            if(testInst.isImmediate()) {
                testInst.setImmediateValue(requireIntegerToken(tokens, testInst, 15));
            } else {
                testInst.setSecond(requireReadableRegister(tokens, testInst));
            }
            instruction = testInst;
        } else if (type.getInstructionClass() == LOAD_STORE) {
            LoadStoreInstruction loadInst = new LoadStoreInstruction(type, labelToken, instructionToken);

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
            LoadStoreRegisterInstruction loadInst = new LoadStoreRegisterInstruction(type, labelToken, instructionToken);

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
            JumpRegisterInstruction jumpInst = new JumpRegisterInstruction(type, labelToken, instructionToken);
            MTMCToken register = requireReadableRegister(tokens, jumpInst);
            jumpInst.setRegister(register);
            instruction = jumpInst;
        } else if (type.getInstructionClass() == JUMP) {
            JumpInstruction jumpInst = new JumpInstruction(type, labelToken, instructionToken);
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

        if (labelToken != null) {
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

    private LinkedList<MTMCToken> handleSyntheticInstructions(LinkedList<MTMCToken> tokens) {
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

    private static MTMCToken maybeGetLabel(LinkedList<MTMCToken> tokens) {
        MTMCToken label = null;
        if (tokens.getFirst().type() == LABEL) {
            label = tokens.poll();
        }
        return label;
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

    private MTMCToken requireIntegerToken(LinkedList<MTMCToken> tokens,
                                          Instruction inst,
                                          int max) {
        MTMCToken token = tokens.poll();
        if (token == null) {
            inst.addError("Integer value required");
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
        TEXT,
    }

}
