package edu.montana.cs.mtmc.asm;

import edu.montana.cs.mtmc.asm.data.Data;
import edu.montana.cs.mtmc.asm.instructions.*;
import edu.montana.cs.mtmc.emulator.Registers;
import edu.montana.cs.mtmc.tokenizer.MTMCToken;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.cs.mtmc.asm.instructions.InstructionType.InstructionClass.*;
import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.*;

public class Assembler {

    List<Instruction> instructions = new ArrayList<>();
    List<Data> data = new ArrayList<>();
    MTMCTokenizer tokenizer;

    public record AssemblyResult(short[] code, short[] data, List<Instruction.Error> errors, String source) {}

    public AssemblyResult assemble(String asm) {
        tokenizer = new MTMCTokenizer(asm, "#");
        while (tokenizer.more()) {
            consumeLine();
        }
        List<Instruction.Error> errors = collectErrors();
        short[] code = null;
        short[] data = null;
        if(errors.isEmpty()) {
            resolveLocations();
            code = new short[instructions.size()];
            for (Instruction instruction : instructions) {
                instruction.genCode(code);
            }
        }
        return new AssemblyResult(code, data, errors, asm);
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
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            instruction.setLocation(i);
        }
        // TODO resolve labels
    }

    private void consumeLine() {

        LinkedList<MTMCToken> tokens = getTokensForLine();
        MTMCToken label = maybeGetLabel(tokens);
        MTMCToken instruction = tokens.poll();

        if (instruction == null || instruction.getType() != IDENTIFIER) {
            instructions.add(new ErrorInstruction(label, instruction, "Invalid Token"));
            return;
        }
        InstructionType type = InstructionType.fromString(instruction.getStringValue());

        if (type == null) {
            instructions.add(new ErrorInstruction(label, instruction, "Unknown instruction type: " + instruction.getStringValue()));
            return;
        }

        if (type.getInstructionClass() == MISC) {
            MiscInstruction misc = new MiscInstruction(type, label, instruction);
            if (type == InstructionType.SYS) {
                MTMCToken sysCallType = requireSysCall(tokens, misc);
                misc.setSyscallType(sysCallType);
            } else if (type == InstructionType.MV) {
                MTMCToken toRegister = requireWriteableRegister(tokens, misc);
                misc.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, misc);
                misc.setFrom(fromRegister);
            } else if (type == InstructionType.NOOP) {/* no args */}
            instructions.add(misc);
        } else if (type.getInstructionClass() == ALU) {
            ALUInstruction aluInst = new ALUInstruction(type, label, instruction);
            if (aluInst.isBinary()) {
                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);
                MTMCToken fromRegister = requireReadableRegister(tokens, aluInst);
                aluInst.setFrom(fromRegister);
            } else {
                MTMCToken toRegister = requireWriteableRegister(tokens, aluInst);
                aluInst.setTo(toRegister);
            }
            instructions.add(aluInst);
        } else if (type.getInstructionClass() == STACK) {
            StackInstruction stackInst = new StackInstruction(type, label, instruction);
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
            instructions.add(stackInst);
        } else if (type.getInstructionClass() == STACK_IMMEDIATE) {
            StackImmediateInstruction stackImmediateInst = new StackImmediateInstruction(type, label, instruction);
            MTMCToken valueToken = requireIntegerToken(tokens, stackImmediateInst, StackImmediateInstruction.MAX);
            stackImmediateInst.setValue(valueToken);
            // if there is a stack register specified, consume it
            if (!tokens.isEmpty() && tokens.peekFirst().getType() == IDENTIFIER) {
                MTMCToken stackReg = requireReadableRegister(tokens, stackImmediateInst);
                stackImmediateInst.setStackRegister(stackReg);
            }
            instructions.add(stackImmediateInst);
        } else if (type.getInstructionClass() == LOAD) {
            LoadInstruction loadInst = new LoadInstruction(type, label, instruction);

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
            instructions.add(loadInst);
        } else if (type.getInstructionClass() == LOAD_IMMEDIATE) {
            LoadImmediateInstruction loadImmediateInst = new LoadImmediateInstruction(type, label, instruction);
            MTMCToken tempRegister = requireTempRegister(tokens, loadImmediateInst);
            loadImmediateInst.setTempRegister(tempRegister);

            MTMCToken valueToken = requireIntegerToken(tokens, loadImmediateInst, StackImmediateInstruction.MAX);
            loadImmediateInst.setValue(valueToken);
            instructions.add(loadImmediateInst);
        } else if (type.getInstructionClass() == JUMP) {
            JumpInstruction stackImmediateInst = new JumpInstruction(type, label, instruction);
            MTMCToken valueToken = requireIntegerToken(tokens, stackImmediateInst, StackImmediateInstruction.MAX);
            stackImmediateInst.setAddressToken(valueToken);
            instructions.add(stackImmediateInst);
        }

        // add error if any tokens are left on the line
        if (!tokens.isEmpty()) {
            MTMCToken token = tokens.poll();
            instructions.add(new ErrorInstruction(null, token, "Unexpected Token"));
        }
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

    @Nullable
    private static MTMCToken maybeGetLabel(LinkedList<MTMCToken> tokens) {
        MTMCToken label = null;
        if (tokens.getFirst().getType() == LABEL) {
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

    enum ASMMode {
        DATA,
        CODE
    }

}
