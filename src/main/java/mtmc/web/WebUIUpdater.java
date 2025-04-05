package mtmc.web;

import com.google.gson.Gson;
import mtmc.emulator.MTMCObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WebUIUpdater implements MTMCObserver {

    private final WebServer webServer;
    private final Gson json = new Gson();

    // UI update infrastructure
    Thread updateThread;
    AtomicInteger updateFlags = new AtomicInteger(0);

    int UPDATE_REGISTER_UI = 0x001;
    int UPDATE_MEMORY_UI = 0x100;
    int UPDATE_DISPLAY_UI = 0x010;

    public static final int UI_UPDATE_INTERVAL = 100;

    public WebUIUpdater(WebServer webServer) {
        this.webServer = webServer;
    }

    public void start() {
        updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(UI_UPDATE_INTERVAL);
                    Map<String, String> uisToUpdate = new HashMap<>();
                    int updates = updateFlags.getAndUpdate(_ -> 0); // get and zero out any changes
                    if (updates != 0) {
                        if ((updates & UPDATE_REGISTER_UI) != 0) {
                            uisToUpdate.put("register-panel", webServer.render("templates/registers.html"));
                        }
                        if ((updates & UPDATE_MEMORY_UI) != 0) {
                            uisToUpdate.put("memory-table", webServer.getComputerView().getMemoryTable());
                        }
                        if ((updates & UPDATE_DISPLAY_UI) != 0) {
                            uisToUpdate.put("display-panel", webServer.render("templates/display.html"));
                        }
                        webServer.sendEvent("update", json.toJson(uisToUpdate));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.start();

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
    public void displayUpdated(int address, byte value) {
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

    public void updateRegistersImmediately() {
        webServer.sendEvent("update", json.toJson(Map.of("register-panel", webServer.render("templates/registers.html"))));
    }

    public void updateMemoryImmediately() {
        webServer.sendEvent("update", json.toJson(Map.of("memory-panel", webServer.render("templates/memory.html"))));
    }

    public void updateDisplayImmediately() {
        webServer.sendEvent("update", json.toJson(Map.of("display-panel", webServer.render("templates/display.html"))));
    }
}
