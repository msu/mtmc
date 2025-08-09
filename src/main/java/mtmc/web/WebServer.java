package mtmc.web;

import com.google.gson.Gson;
import io.javalin.http.HttpStatus;
import mtmc.emulator.MonTanaMiniComputer;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.*;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import mtmc.os.fs.FileSystem;

public class WebServer {

    public static final int IDEAL_PORT = 8080;
    public static final String SERVER_URL = "http://localhost:" + IDEAL_PORT;
    private static WebServer instance;

    private final MonTanaMiniComputer computer;
    private Javalin javalinApp;
    PebbleEngine templateEngine;

    private final MTMCWebView computerView;
    Queue<SseClient> sseClients = new ConcurrentLinkedQueue<SseClient>();
    Gson json = new Gson();

    WebUIUpdater uiUpdater = new WebUIUpdater(this);

    public WebServer(MonTanaMiniComputer computer) {
        touchLogFile(computer);
        this.computer = computer;
        this.computerView = new MTMCWebView(computer);
        this.templateEngine = new PebbleEngine.Builder().cacheActive(true).build();
        initRoutes();
        this.computer.addObserver(uiUpdater);
        uiUpdater.start();
    }

    private void touchLogFile(MonTanaMiniComputer computer) {
        try {
            File file = computer.getOS().loadFile("/logs/sys.log");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                Files.createFile(file.toPath());
            }
        } catch (FileAlreadyExistsException e) {
            // ignore
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initRoutes() {

        // config
        this.javalinApp = Javalin.create(cfg -> {
            cfg.staticFiles.add("public");
            cfg.staticFiles.enableWebjars();
        });
        // paths
        javalinApp
                .get("/", ctx -> {
                    ctx.html(render("templates/index.html"));
                })
                .get("/asm", ctx -> {
                    ctx.header("Content-Type", "text/x-asm");
                    ctx.result(computerView.getAssemblySource());
                })
                .get("/display", ctx -> {
                    ctx.header("Content-Type", "image/png");
                    ctx.result(computer.getDisplay().toPng());
                })
                .post("/breakpoint/{line}/{active}", ctx -> {
                    int line = Integer.parseInt(ctx.pathParam("line"));
                    boolean active = Boolean.parseBoolean(ctx.pathParam("active"));
                    boolean set = computerView.setBreakpoint(line, active);
                    ctx.html(String.valueOf(set));
                })
                .post("/cmd", ctx -> {
                    Map vals = json.fromJson(ctx.body(), Map.class);
                    String cmd = (String) vals.get("cmd");
                    computer.getConsole().resetOutput();
                    computer.getOS().processCommand(cmd);
                    String output = computer.getConsole().getOutput();
                    sendEvent("console-output", output);
                    sendEvent("console-ready", "");
                    ctx.html("");
                })
                .post("/readchar", ctx -> {
                    Map vals = json.fromJson(ctx.body(), Map.class);
                    String str = (String) vals.get("c");
                    computer.getConsole().setCharValue(str.charAt(0));
                    sendEvent("console-ready", "mtmc$");
                    ctx.html("");
                })
                .post("/readint", ctx -> {
                    Map vals = json.fromJson(ctx.body(), Map.class);
                    int value = Integer.parseInt((String)vals.get("str"));
                    computer.getConsole().setShortValue((short)value);
                    sendEvent("console-ready", "mtmc$");
                    ctx.html("");
                })
                .post("/readstr", ctx -> {
                    Map vals = json.fromJson(ctx.body(), Map.class);
                    String str = (String) vals.get("str");
                    computer.getConsole().setReadString(str);
                    sendEvent("console-ready", "mtmc$");
                    ctx.html("");
                })
                .post("/speed", ctx -> {
                    String speed = ctx.formParam("speed");
                    int speedi = Integer.parseInt(speed);
                    computer.setSpeed(speedi);
                    ctx.html(render("templates/control.html"));
                })
                .post("/control/{action}", ctx -> {
                    if (ctx.pathParam("action").equals("reset")) {
                        computer.initMemory();
                    }
                    if (ctx.pathParam("action").equals("pause")) {
                        computer.pause();
                        sendEvent("console-ready", "mtmc$");
                    }
                    if (ctx.pathParam("action").equals("run")) {
                        computerView.applyBreakpoints();
                        computer.run();
                    }
                    if (ctx.pathParam("action").equals("step")) {
                        computer.step();
                    }
                    if (ctx.pathParam("action").equals("back")) {
                        computer.back();
                    }
                    ctx.html(render("templates/control.html"));
                })
                .post("/io/{buttons}", ctx -> {
                    int buttons = Integer.parseInt(ctx.pathParam("buttons"), 16);
                    computer.getIO().setValue(buttons);
                })
                .post("/memFormat", ctx -> {
                    computerView.toggleMemoryFormat();
                    uiUpdater.updateMemoryImmediately();
                })
                .get("/fs/read/*", ctx -> {
                    String path = ctx.path().substring("/fs/read".length());
                    FileSystem fs = computerView.getFileSystem();

                    ctx.contentType(fs.getMimeType(path));
                    ctx.result(fs.openFile(path));
                })
                .post("/fs/write/*", ctx -> {
                    String path = ctx.path().substring("/fs/write".length());
                    FileSystem fs = computerView.getFileSystem();
                    
                    fs.saveFile(path, ctx.bodyInputStream());
                    ctx.html("");
                })
                .get("/fs/toggle/*", ctx -> {
                    String path = ctx.path().substring("/fs/toggle/".length());
                    computerView.getFileSystem().setCWD(path);
                    ctx.html(render("templates/filetree.html"));

                    //ctx.html(computerView.getVisualShell());
                    // .getVisualShell() is outdated. renderFileTree() is manual input.
                })
                .get("/fs/open/*", ctx -> {
                    String path = ctx.path().substring("/fs/open/".length());
                    boolean successfullyOpened = computerView.openFile(path);
                    if (!successfullyOpened) {
                        ctx.status(HttpStatus.NOT_FOUND);
                    }
                    ctx.html(render(computerView.selectEditor()));
                })
                .post("/fs/cwd", ctx -> {
                    computerView.getFileSystem().setCWD(ctx.formParam("cwd"));
                    ctx.html(render("templates/filetree.html"));
                })
                .get("/fs/close", ctx -> {
                    computerView.closeFile();
                    ctx.html(render("templates/filetree.html"));
                })
                .get("/fs/new/file", ctx -> {
                    ctx.html(render("templates/newfile.html"));
                })
                .get("/fs/new/dir", ctx -> {
                    ctx.html(render("templates/newdir.html"));
                })
                .post("/fs/create/file", ctx -> {
                    if (computerView.createFile(ctx.formParam("filename"), ctx.formParam("mime"))) {
                        ctx.html(render(computerView.selectEditor()));
                    } else {
                        ctx.html(render("templates/newfile.html"));
                    }
                })
                .post("/fs/create/dir", ctx -> {
                    if (computerView.createDirectory(ctx.formParam("filename"))) {
                        computerView.closeFile();
                        ctx.html(render("templates/filetree.html"));
                    } else {
                        ctx.html(render("templates/newdir.html"));
                    }
                })
                .sse("/sse", client -> {
                    client.keepAlive();
                    client.onClose(() -> sseClients.remove(client));
                    sseClients.add(client);
                });

        // start server
        int safePort = findSafePort();
        javalinApp.start(safePort);
    }

    private int findSafePort() {
        int port = IDEAL_PORT;
        while (port < 0xFFFF) {
            try(ServerSocket serverSocket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                port = port + 1;
            }
        }
        throw new IllegalStateException("Could not find a safe port!");
    }

    void sendEvent(String update, String data) {
        sseClients.forEach(c -> {
            c.sendEvent(update, data);
        });
    }

    String render(String templateName) {
        PebbleTemplate template = templateEngine.getTemplate(templateName);
        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, Map.of("computer", computerView));
            String string = writer.toString();
            return string;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static WebServer getInstance(MonTanaMiniComputer computer) {
        maybeInit(computer);
        return instance;
    }

    private static void maybeInit(MonTanaMiniComputer computer) {
        if (instance == null) {
            instance = new WebServer(computer);
        }
    }

    public URI getURL() throws URISyntaxException {
        return new URI(SERVER_URL);
    }

    public static void main(String[] args) {
        MonTanaMiniComputer computer = new MonTanaMiniComputer();
        maybeInit(computer);
    }


    public MTMCWebView getComputerView() {
        return computerView;
    }
}
