package mtmc.os.shell;

import mtmc.asm.Assembler;
import mtmc.asm.AssemblyResult;
import mtmc.asm.instructions.Instruction;
import mtmc.emulator.DebugInfo;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.emulator.Register;
import mtmc.os.exec.Executable;
import mtmc.os.shell.builtins.*;
import mtmc.tokenizer.MTMCToken;

import static mtmc.tokenizer.MTMCToken.TokenType.*;

import mtmc.tokenizer.MTMCTokenizer;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import mtmc.os.fs.FileSystem;

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
        COMMANDS.put("sc", new SeacCommand());
    }

    public static boolean isCommand(String cmd) {
        return COMMANDS.containsKey(cmd.toLowerCase());
    }
    
    private static boolean findExecutable(String path, FileSystem fs) {
        if (path == null || path.equals("")) return false;
        if (fs.exists(path) && !fs.listFiles(path).directory) return true;
        if (fs.exists("/bin/" + path) && !fs.listFiles("/bin/" + path).directory) return true;
        
        return false;
    }
    
    private static void runExecutable(String file, String command, MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        FileSystem fs = computer.getFileSystem();
        Path srcPath = Path.of("disk/" + fs.resolve(file));
        if(!srcPath.toFile().exists()) {
            srcPath = Path.of("disk" + fs.resolve("/bin/" + file));
        }
        Executable exec = Executable.load(srcPath);
        computer.load(exec.code(), exec.data(), exec.graphics(), exec.debugInfo());
        tokens.consume();
        String arg = command.substring(file.length()).strip();
        computer.getOS().applyBreakpoints();
        computer.setArg(arg);
        computer.run();
    }
    
    public static void execCommand(String command, MonTanaMiniComputer computer) {
        MTMCTokenizer tokens = new MTMCTokenizer(command, "#");
        try {
            MTMCToken identifier = tokens.matchAndConsume(IDENTIFIER);
            String cmd;
            if (identifier == null) {
                MTMCToken question = tokens.matchAndConsume(QUESTION_MARK);
                String executable = tokens.collapseTokensAsString();
                
                // alias ? to help
                if (question != null) {
                    cmd = "help";
                } else if (findExecutable(executable, computer.getFileSystem())) {
                    cmd = executable;
                } else {
                    printShellHelp(computer);
                    return;
                }
            } else {
                cmd = identifier.stringValue();
            }
            if (isCommand(cmd)) {
                COMMANDS.get(cmd.toLowerCase()).exec(tokens, computer);
            } else {
                tokens.reset();
                LinkedList<MTMCToken> asm = new LinkedList<>(tokens.stream().toList());
                LinkedList<MTMCToken> updatedAsm = Assembler.transformSyntheticInstructions(asm);
                MTMCToken firstToken = updatedAsm.peekFirst();
                String firstTokenStr = firstToken.stringValue();
                if (!updatedAsm.isEmpty() && Instruction.isInstruction(firstTokenStr)) {
                    Assembler assembler = new Assembler();
                    AssemblyResult result = assembler.assemble(command);
                    if (result.errors().isEmpty()) {
                        byte[] code = result.code();
                        if (code.length == 4) {
                            int data = (code[2] << 8) | code[3];
                            computer.setRegisterValue(Register.DR, data);
                        }
                        int lower = code[1] & 0xFF;
                        int higher = code[0] & 0xFF;
                        int inst = (higher << 8) | lower;
                        DebugInfo originalDebugInfo = computer.getDebugInfo();
                        computer.setDebugInfo(result.debugInfo());
                        computer.execInstruction((short) inst);
                        computer.setDebugInfo(originalDebugInfo);
                    } else {
                        computer.getConsole().println(result.printErrors());
                    }
                } else {
                    if (findExecutable(cmd, computer.getFileSystem())) {
                        runExecutable(cmd, command, tokens, computer);
                    } else {
                        printShellHelp(computer);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            computer.getConsole().println("No such file: " + e.getFile());
        } catch (Exception e) {
            computer.getConsole().println(e.getMessage());
            e.printStackTrace();
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
