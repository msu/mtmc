package mtmc.web;

import com.google.gson.Gson;
import java.util.Base64;
import mtmc.emulator.MTMCObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import mtmc.emulator.MTMCConsole;

public class WebUIUpdater implements MTMCObserver {

    private final WebServer webServer;
    private final Gson json = new Gson();
    private long lastUpdate = 0;
    private long lastConsoleUpdate = 0;

    // UI update infrastructure
    Thread updateThread;
    AtomicInteger updateFlags = new AtomicInteger(0xFFFFFFFF); // Force a full update on restart

    int UPDATE_REGISTER_UI   = 0x000001;
    int UPDATE_MEMORY_UI     = 0x000100;
    int UPDATE_DISPLAY_UI    = 0x000010;
    int UPDATE_FILESYSTEM_UI = 0x001000;
    int UPDATE_EXECUTION_UI  = 0x010000;

    public static final int UI_UPDATE_INTERVAL = 100;     // Approximately 10 FPS
    public static final int DISPLAY_UPDATE_INTERVAL = 16; // Approximately 60 FPS

    public WebUIUpdater(WebServer webServer) {
        this.webServer = webServer;
    }
    
    private String getDataURL(byte[] data) {
        StringBuilder buffer = new StringBuilder("data:image/png;base64,");
        String base64 = Base64.getEncoder().encodeToString(data);
        
        buffer.append(base64);
        
        return buffer.toString();
    }
    
    private String getEncodedDisplay() {
        byte[] data = webServer.getComputerView().getDisplay().toPng();
        
        return getDataURL(data);
    }
    
    private String getConsoleOutput() {
        MTMCConsole console = webServer.getComputerView().getConsole();
        long time = System.currentTimeMillis();
        long delta = time - lastConsoleUpdate;
        
        if (delta < DISPLAY_UPDATE_INTERVAL) {
            try { Thread.sleep(DISPLAY_UPDATE_INTERVAL - delta); } catch(InterruptedException e) {}
        }
        
        lastConsoleUpdate = time;
        
        return console.consumeLines();
    }
    
    private String getConsolePartial() {
        MTMCConsole console = webServer.getComputerView().getConsole();
        long time = System.currentTimeMillis();
        long delta = time - lastConsoleUpdate;
        
        if (delta < DISPLAY_UPDATE_INTERVAL) {
            try { Thread.sleep(DISPLAY_UPDATE_INTERVAL - delta); } catch(InterruptedException e) {}
        }
        
        return console.getOutput();
    }

    public void start() {
        updateThread = new Thread(() -> {
            while (true) {
                
                if (webServer.sseClients.size() < 1) continue; // Save the update until they reconnect
                
                try {
                    Thread.sleep(DISPLAY_UPDATE_INTERVAL);
                    Map<String, String> uisToUpdate = new HashMap<>();
                    boolean update = (lastUpdate + UI_UPDATE_INTERVAL) <= System.currentTimeMillis();
                    int updates = updateFlags.getAndUpdate(value -> update ? 0 : value & ~(UPDATE_DISPLAY_UI)); // get and zero out any changes

                    if ((updates & UPDATE_DISPLAY_UI) != 0) {
                        webServer.sendEvent("update:display", getEncodedDisplay());
                        updates &= ~UPDATE_DISPLAY_UI;
                    }
                    
                    if(!update) continue;

                    if (updates != 0) {
                        if ((updates & UPDATE_REGISTER_UI) != 0) {
                            webServer.sendEvent("update:registers", webServer.render("templates/registers.html"));
                        }
                        if ((updates & UPDATE_MEMORY_UI) != 0) {
                            webServer.sendEvent("update:memory", webServer.getComputerView().getMemoryTable());
                        }
                        if ((updates & UPDATE_FILESYSTEM_UI) != 0) {
                            webServer.sendEvent("update:filesystem", webServer.render("templates/filetree.html"));
                        }
                        if ((updates & UPDATE_EXECUTION_UI) != 0) {
                            webServer.sendEvent("update:execution", webServer.render("templates/control.html"));
                        }
                    }
                    
                    lastUpdate = System.currentTimeMillis();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.start();

    }
    
    @Override
    public void consoleUpdated() {
        webServer.sendEvent("console-output", getConsoleOutput());
    }
    
    @Override
    public void consolePrinting() {
        webServer.sendEvent("console-partial", getConsolePartial());
    }
    
    @Override
    public void executionUpdated() {
        updateFlags.updateAndGet(operand -> operand | UPDATE_EXECUTION_UI);
    }
    
    @Override
    public void filesystemUpdated() {
        if (webServer.getComputerView().hasFileOpen()) {
            return; // Don't update the file tree if an editor is open
        }
        updateFlags.updateAndGet(operand -> operand | UPDATE_FILESYSTEM_UI);
    }

    @Override
    public void registerUpdated(int register, int value) {
        // always update memory and register uis on a register update
        updateFlags.updateAndGet(operand -> operand | UPDATE_REGISTER_UI | UPDATE_MEMORY_UI);
    }

    @Override
    public void memoryUpdated(int address, byte value) {
        updateFlags.updateAndGet(operand -> operand | UPDATE_MEMORY_UI);
    }

    @Override
    public void displayUpdated() {
        updateFlags.updateAndGet(operand -> operand | UPDATE_DISPLAY_UI);
    }

    @Override
    public void instructionFetched(short instruction) {
        // ignore for now
    }

    @Override
    public void beforeExecution(short instruction) {
        // ignore for now
    }

    @Override
    public void afterExecution(short instruction) {
        // ignore for now
    }

    @Override
    public void stepExecution() {
        if (!webServer.getComputerView().hasDebugInfo()) {
            return;
        }
        
        var program = webServer.getComputerView().getProgram();
        var asm = webServer.getComputerView().getAssemblyLine();
        var source = webServer.getComputerView().getSourceLine();
        var step = new ExecutionStep(program, asm, source);
        
        webServer.sendEvent("update:step-execution", json.toJson(step));
    }

    @Override
    public void computerReset() {
        updateFlags.updateAndGet(operand -> operand | UPDATE_DISPLAY_UI | UPDATE_MEMORY_UI | UPDATE_REGISTER_UI);
    }

    public void updateMemoryImmediately() {
        webServer.sendEvent("update:memory-panel", webServer.render("templates/memory.html"));
    }
    
    @Override
    public void requestCharacter() {
        webServer.sendEvent("console-readchar", ">");
    }
    
    @Override
    public void requestInteger() {
        webServer.sendEvent("console-readint", "#");
    }
    
    @Override
    public void requestString() {
        webServer.sendEvent("console-readstr", ">");
    }
    
    private record ExecutionStep(String program, int asm, int src) {};
}
