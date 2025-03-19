package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.asm.data.Data;
import edu.montana.cs.mtmc.asm.instructions.*;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.cs.mtmc.asm.instructions.InstructionType.InstructionClass.*;
import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.*;

public class Assembler {

    List<Instruction> instructions = new ArrayList<>();
    int instructionsSize = 0;
    List<Data> data = new ArrayList<>();
    int dataSize = 0;
    HashMap<String, HasLocation> labels = new HashMap<>();

    MTMCTokenizer tokenizer;

    public AssemblyResult assemble(String asm) {
        tokenizer = new MTMCTokenizer(asm, "#");
        parseAssembly();
        resolveLocations();
        List<Instruction.Error> errors = collectErrors();
        byte[] code = null, data = null;
        if(errors.isEmpty()) {
            code = codeGen();
            data = dataGen();
        }
        return new AssemblyResult(code, data, errors, asm);
    }

    private void parseAssembly() {
        while (tokenizer.more()) {
            parseLine();
        }
    }

    private byte[] dataGen() {
        byte[] dataBytes = new byte[dataSize];
        for (Data dataElt : data) {
            dataElt.genCode(dataBytes, this);
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

    private List<Instruction.Error> collectErrors() {
        List<Instruction.Error> errors = new ArrayList<>();
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
        MTMCToken labelToken = maybeGetLabel(tokens);
        MTMCToken instructionToken = tokens.poll();

        if (instructionToken == null || instructionToken.getType() != IDENTIFIER) {
            instructions.add(new ErrorInstruction(labelToken, instructionToken, "Invalid Token"));
            return;
        }
        InstructionType type = InstructionType.fromString(instructionToken.getStringValue());

        if (type == null) {
            instructions.add(new ErrorInstruction(labelToken, instructionToken, "Unknown instructionToken type: " + instructionToken.getStringValue()));
            return;
        }

        Instruction instruction;
        if (type.getInstructionClass() == MISC) {
            MiscInstruction miscInst = new MiscInstruction(type, labelToken, instructionToken);
            if (type == InstructionType.SYS) {
                MTMCToken sysCallType = requireSysCall(tokens, miscInst);
                miscInst.setSyscallType(sysCallType);
            } else if (type == InstructionType.MV) {
                MTMCToken toRegister = requireWriteableRegister(tokens, miscInst);
                miscInst.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, miscInst);
                miscInst.setFrom(fromRegister);
            } else if (type == InstructionType.NOOP) {/* no args */}
            instruction = miscInst;
        } else if (type.getInstructionClass() == ALU) {
            ALUInstruction aluInst = new ALUInstruction(type, labelToken, instructionToken);
            if (aluInst.isBinary()) {
                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, aluInst);
                aluInst.setFrom(fromRegister);
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
            } else if(type == InstructionType.POP) {
                MTMCToken toRegister = requireWriteableRegister(tokens, stackInst);
                stackInst.setTarget(toRegister);
            } else if (type == InstructionType.SOP) {
                MTMCToken aluOp = requireAluOp(tokens, stackInst);
                stackInst.setALUOp(aluOp);
            }
            // if there is a stack register specified, consume it
            if (!tokens.isEmpty() && tokens.peekFirst().getType() == IDENTIFIER) {
                MTMCToken stackReg = requireReadableRegister(tokens, stackInst);
                stackInst.setStackRegister(stackReg);
            }
            instruction = stackInst;
        } else if (type.getInstructionClass() == STACK_IMMEDIATE) {
            StackImmediateInstruction stackImmediateInst = new StackImmediateInstruction(type, labelToken, instructionToken);
            MTMCToken valueToken = requireIntegerToken(tokens, stackImmediateInst, StackImmediateInstruction.MAX);
            stackImmediateInst.setValue(valueToken);
            // if there is a stack register specified, consume it
            if (!tokens.isEmpty() && tokens.peekFirst().getType() == IDENTIFIER) {
                MTMCToken stackReg = requireReadableRegister(tokens, stackImmediateInst);
                stackImmediateInst.setStackRegister(stackReg);
            }
            instruction = stackImmediateInst;
        } else if (type.getInstructionClass() == LOAD) {
            LoadInstruction loadInst = new LoadInstruction(type, labelToken, instructionToken);

            // target register must b
            MTMCToken targetReg;
            if (type == InstructionType.LW || type == InstructionType.LB) {
                targetReg = requireWriteableRegister(tokens, loadInst);
            } else {
                targetReg = requireReadableRegister(tokens, loadInst);
            }
            loadInst.setTargetToken(targetReg);
            MTMCToken pointerReg = requireReadableRegister(tokens, loadInst);
            loadInst.setPointerToken(pointerReg);

            // if there is an offset register specified, consume it
            if (!tokens.isEmpty() && tokens.peekFirst().getType() == IDENTIFIER) {
                MTMCToken offsetReg = requireReadableRegister(tokens, loadInst);
                loadInst.setOffsetToken(offsetReg);
            }
            instruction = loadInst;
        } else if (type.getInstructionClass() == LOAD_IMMEDIATE) {
            LoadImmediateInstruction loadImmediateInst = new LoadImmediateInstruction(type, labelToken, instructionToken);
            MTMCToken tempRegister = requireTempRegister(tokens, loadImmediateInst);
            loadImmediateInst.setTempRegister(tempRegister);
            MTMCToken labelValue = maybeGetLabelReference(tokens);
            MTMCToken valueToken;
            if (labelValue != null) {
                valueToken = labelValue;
            } else {
                valueToken = requireIntegerToken(tokens, loadImmediateInst, LoadImmediateInstruction.MAX);
            }
            loadImmediateInst.setValue(valueToken);
            instruction = loadImmediateInst;
        } else if (type.getInstructionClass() == JUMP) {
            JumpInstruction jumpInst = new JumpInstruction(type, labelToken, instructionToken);
            MTMCToken labelValue = maybeGetLabelReference(tokens);
            MTMCToken valueToken;
            if (labelValue != null) {
                valueToken = labelValue;
            } else {
                valueToken = requireIntegerToken(tokens, jumpInst, LoadImmediateInstruction.MAX);
            }
            jumpInst.setAddressToken(valueToken);
            instruction = jumpInst;
        } else {
            instruction = new ErrorInstruction(null, instructionToken, "Unexpected Token");
        }

        if (labelToken != null) {
            if (hasLabel(labelToken.getStringValue())) {
                instruction.addError(tokens.poll(), "Label already defined: " + labelToken.getStringValue());
            } else {
                labels.put(labelToken.getLabelValue(), instruction);
            }
        }

        // add error if any tokens are left on the line
        if (!tokens.isEmpty()) {
            instruction.addError(tokens.poll(), "Unexpected Token");
        }

        instructions.add(instruction);
    }

    //===================================================
    // tokenization helper functions
    //===================================================
    private MTMCToken requireSysCall(LinkedList<MTMCToken> tokens, Instruction inst) {
        MTMCToken sysCallType = tokens.poll();
        if (sysCallType == null) {
            inst.addError("Syscall required");
        } else if (sysCallType.getType() != IDENTIFIER) {
            inst.addError(sysCallType, "Syscall required");
        } else if (!MiscInstruction.isSysCall(sysCallType.getStringValue())) {
            inst.addError(sysCallType, "Unknown syscall : " + sysCallType.getStringValue());
        }
        return sysCallType;
    }

    private MTMCToken requireAluOp(LinkedList<MTMCToken> tokens, Instruction inst) {
        MTMCToken sysCallType = tokens.poll();
        if (sysCallType == null) {
            inst.addError("Syscall required");
        } else if (sysCallType.getType() != IDENTIFIER) {
            inst.addError(sysCallType, "Syscall required");
        } else if (!ALUInstruction.isALUInstruction(sysCallType.getStringValue())) {
            inst.addError(sysCallType, "Unknown alu operation : " + sysCallType.getStringValue());
        }
        return sysCallType;
    }

    private static MTMCToken maybeGetLabel(LinkedList<MTMCToken> tokens) {
        MTMCToken label = null;
        if (tokens.getFirst().getType() == LABEL) {
            label = tokens.poll();
        }
        return label;
    }

    private static MTMCToken maybeGetLabelReference(LinkedList<MTMCToken> tokens) {
        MTMCToken label = null;
        if (tokens.getFirst().getType() == IDENTIFIER) {
            label = tokens.poll();
        }
        return label;
    }

    private MTMCToken requireWriteableRegister(LinkedList<MTMCToken> tokens, Instruction instruction) {
        MTMCToken nextToken = tokens.poll();
        if(nextToken == null) {
            instruction.addError("Register required");
        } else if(nextToken.getType() != IDENTIFIER) {
            instruction.addError(nextToken, "Invalid Register : " + nextToken.getStringValue());
        } else if (!Registers.isWriteable(nextToken.getStringValue())) {
            instruction.addError(nextToken, "Register not writeable : " + nextToken.getStringValue());
        }
        return nextToken;
    }

    private MTMCToken requireTempRegister(LinkedList<MTMCToken> tokens, Instruction instruction) {
        MTMCToken nextToken = tokens.poll();
        if(nextToken == null) {
            instruction.addError("Register required");
        } else if(nextToken.getType() != IDENTIFIER) {
            instruction.addError(nextToken, "Invalid Register : " + nextToken.getStringValue());
        } else if (!Registers.isTempRegister(nextToken.getStringValue())) {
            instruction.addError(nextToken, "Register not writeable : " + nextToken.getStringValue());
        }
        return nextToken;
    }

    private MTMCToken requireReadableRegister(LinkedList<MTMCToken> tokens, Instruction instruction) {
        MTMCToken nextToken = tokens.poll();
        if(nextToken == null) {
            instruction.addError("Register required");
        } else if(nextToken.getType() != IDENTIFIER) {
            instruction.addError(nextToken, "Invalid Register : " + nextToken.getStringValue());
        } else if (!Registers.isReadable(nextToken.getStringValue())) {
            instruction.addError(nextToken, "Register not readable : " + nextToken.getStringValue());
        }
        return nextToken;
    }

    private MTMCToken requireIntegerToken(LinkedList<MTMCToken> tokens,
                                          Instruction inst,
                                          int max) {
        MTMCToken token = tokens.poll();
        if (token == null) {
            inst.addError("Integer value required");
        } else if (token.getType() == INTEGER || token.getType() == HEX || token.getType() == BINARY) {
            Integer integerValue = token.getIntegerValue();
            if (integerValue < 0 || max < integerValue) {
                inst.addError(token, "Integer value out of range: 0-" + max);
            }
        } else {
            inst.addError(token, "Integer value expected");
        }
        return token;
    }

    private LinkedList<MTMCToken> getTokensForLine() {
        LinkedList<MTMCToken> tokens = new LinkedList<>();
        if (tokenizer.more()) {
            MTMCToken first = tokenizer.consume();
            tokens.add(first);
            while(tokenizer.more() &&
                    first.getLine() == tokenizer.currentToken().getLine()) {
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

    enum ASMMode {
        DATA,
        CODE
    }

}
