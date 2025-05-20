package mtmc.os.shell;

import mtmc.asm.Assembler;
import mtmc.asm.AssemblyResult;
import mtmc.asm.instructions.Instruction;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.shell.builtins.*;
import mtmc.tokenizer.MTMCToken;

import static mtmc.tokenizer.MTMCToken.TokenType.*;

import mtmc.tokenizer.MTMCTokenizer;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Shell {
    private static final Map<String, ShellCommand> COMMANDS = new LinkedHashMap<>();

    static {
        COMMANDS.put("help", new HelpCommand());
        COMMANDS.put("exit", new ExitCommand());
        COMMANDS.put("set", new SetCommand());
        COMMANDS.put("get", new GetCommand());
        COMMANDS.put("web", new WebCommand());
        COMMANDS.put("disp", new DisplayCommand());
        COMMANDS.put("asm", new AssembleCommand());
        COMMANDS.put("load", new LoadCommand());
        COMMANDS.put("step", new StepCommand());
        COMMANDS.put("run", new RunCommand());
        COMMANDS.put("pause", new PauseCommand());
        COMMANDS.put("speed", new SpeedCommand());
    }

    public static boolean isCommand(String cmd) {
        return COMMANDS.containsKey(cmd);
    }

    public static void execCommand(String command, MonTanaMiniComputer computer) {
        MTMCTokenizer tokens = new MTMCTokenizer(command, "#");
        try {
            MTMCToken identifier = tokens.matchAndConsume(IDENTIFIER);
            String cmd;
            if (identifier == null) {
                // alias ? to help
                if (tokens.matchAndConsume(QUESTION_MARK) != null) {
                    cmd = "help";
                } else {
                    printShellHelp(computer);
                    return;
                }
            } else {
                cmd = identifier.stringValue();
            }
            if (isCommand(cmd)) {
                COMMANDS.get(cmd).exec(tokens, computer);
            } else {
                tokens.reset();
                LinkedList<MTMCToken> asm = new LinkedList<>(tokens.stream().toList());
                LinkedList<MTMCToken> updatedAsm = Assembler.transformSyntheticInstructions(asm);
                if (!updatedAsm.isEmpty() &&
                        Instruction.isInstruction(updatedAsm.peekFirst().stringValue())) {
                    Assembler assembler = new Assembler();
                    AssemblyResult result = assembler.assemble(command);
                    if (result.errors().isEmpty()) {
                        byte[] code = result.code();
                        if (code.length == 4) {
                            int data = (code[2] << 8) | code[3];
                            computer.setRegisterValue(Register.DR, data);
                        }
                        int inst = (code[0] << 8) | code[1];
                        computer.execInstruction((short) inst);
                    } else {
                        computer.getConsole().println(result.printErrors());
                    }
                } else {
                    printShellHelp(computer);
                }
            }
        } catch (Exception e) {
            computer.getConsole().println(e.getMessage());
        }
    }

    public static void printShellHelp(MonTanaMiniComputer computer) {
        computer.getConsole().println("Shell BuiltIns: \n");
        for (ShellCommand value : COMMANDS.values()) {
            computer.getConsole().println(value.getHelp());
        }
        computer.getConsole().println("Also: ");
        computer.getConsole().println("   <asm instruction>");
        computer.getConsole().println("or \n" +
                "  <executable>\n\n");
    }

    public static void printShellWelcome(MonTanaMiniComputer computer) {
        computer.getConsole().println("Welcome to MtOS!  Type ? for help");
    }
}
