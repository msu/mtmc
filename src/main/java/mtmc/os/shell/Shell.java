package mtmc.os.shell;

import mtmc.asm.Assembler;
import mtmc.asm.AssemblyResult;
import mtmc.asm.instructions.Instruction;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.builtins.*;
import mtmc.tokenizer.MTMCToken;

import static mtmc.tokenizer.MTMCToken.TokenType.*;

import mtmc.tokenizer.MTMCTokenizer;

import java.util.LinkedHashMap;
import java.util.Map;

public class Shell {
    private static final Map<String, ShellCommand> COMMANDS = new LinkedHashMap<>();

    static {
        COMMANDS.put("help", new HelpCommand());
        COMMANDS.put("exit", new ExitCommand());
        COMMANDS.put("set", new SetCommand());
        COMMANDS.put("web", new WebCommand());
        COMMANDS.put("display", new DisplayCommand());
        COMMANDS.put("assemble", new AssembleCommand());
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
                if (Instruction.isInstruction(cmd)) {
                    Assembler assembler = new Assembler();
                    AssemblyResult result = assembler.assemble(command);
                    if (result.errors().isEmpty()) {
                        byte[] code = result.code();
                        for (int i = 0; i < code.length; i = i + 2) {
                            int instCodeUpper = code[i] << 8;
                            int instCodeLower = code[i + 1] & 0xFF;
                            short inst = (short) (instCodeUpper | instCodeLower);
                            computer.execInstruction(inst);
                        }
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
