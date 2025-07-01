package mtmc.web;

import com.google.gson.Gson;
import java.util.Base64;
import mtmc.emulator.MTMCObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WebUIUpdater implements MTMCObserver {

    private final WebServer webServer;
    private final Gson json = new Gson();
    private long lastUpdate = 0;

    // UI update infrastructure
    Thread updateThread;
    AtomicInteger updateFlags = new AtomicInteger(0);

    int UPDATE_REGISTER_UI   = 0x00001;
    int UPDATE_MEMORY_UI     = 0x00100;
    int UPDATE_DISPLAY_UI    = 0x00010;
    int UPDATE_FILESYSTEM_UI = 0x01000;
    int UPDATE_EXECUTION_UI  = 0x10000;

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

    public void start() {
        updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(DISPLAY_UPDATE_INTERVAL);
                    Map<String, String> uisToUpdate = new HashMap<>();
                    boolean update = (lastUpdate + UI_UPDATE_INTERVAL) <= System.currentTimeMillis();
                    int updates = updateFlags.getAndUpdate(value -> update ? 0 : value & (~UPDATE_DISPLAY_UI)); // get and zero out any changes

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
                            webServer.sendEvent("update:filesystem", webServer.render("templates/editors.html"));
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
    public void executionUpdated() {
        updateFlags.updateAndGet(operand -> operand | UPDATE_EXECUTION_UI);
    }
    
    @Override
    public void filesystemUpdated() {
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
    public void computerReset() {
        updateFlags.updateAndGet(operand -> operand | UPDATE_DISPLAY_UI | UPDATE_MEMORY_UI | UPDATE_REGISTER_UI);
    }

    public void updateMemoryImmediately() {
        webServer.sendEvent("update:memory-panel", webServer.render("templates/memory.html"));
    }

}
