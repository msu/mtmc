package mtmc.web;

import com.google.gson.Gson;
import io.javalin.http.HttpStatus;
import mtmc.emulator.MTMCIO;
import mtmc.emulator.MonTanaMiniComputer;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebServer {

    public static final int PORT = 8081;
    public static final String SERVER_URL = "http://localhost:" + PORT;
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
        });
        // paths
        javalinApp
                .get("/", ctx -> {
                    ctx.html(render("templates/index.html"));
                })
                .get("/display", ctx -> {
                    ctx.header("Content-Type", "image/png");
                    ctx.result(computer.getDisplay().toPng());
                })
                .post("/cmd", ctx -> {
                    Map vals = json.fromJson(ctx.body(), Map.class);
                    String cmd = (String) vals.get("cmd");
                    computer.getConsole().resetOutput();
                    computer.getOS().processCommand(cmd);
                    String output = computer.getConsole().getOutput();
                    sendEvent("console-output", output);
                    sendEvent("console-ready", "{}");
                    ctx.html("");
                })
                .post("/speed", ctx -> {
                    String speed = ctx.formParam("speed");
                    int speedi = Integer.parseInt(speed);
                    computer.setSpeed(speedi);
                    ctx.html("");
                })
                .post("/control/{action}", ctx -> {
                    if (ctx.pathParam("action").equals("reset")) {
                        computer.initMemory();
                    }
                    if (ctx.pathParam("action").equals("pause")) {
                        computer.pause();
                    }
                    if (ctx.pathParam("action").equals("step")) {
                        computer.setStatus(MonTanaMiniComputer.ComputerStatus.EXECUTING);
                        computer.fetchAndExecute();
                        computer.fetchCurrentInstruction(); // fetch next instruction for display
                    }
                })
                .post("/io/{button}/{action}", ctx -> {
                    MTMCIO io = computer.getIO();
                    if (ctx.pathParam("action").equals("pressed")) {
                        io.keyPressed(ctx.pathParam("button"));
                    }
                    if (ctx.pathParam("action").equals("released")) {
                        io.keyReleased(ctx.pathParam("button"));
                    }
                })
                .post("/memFormat", ctx -> {
                    computerView.toggleMemoryFormat();
                    uiUpdater.updateMemoryImmediately();
                })
                .get("/fs/toggle/*", ctx -> {
                    String path = ctx.path().substring("/fs/toggle/".length());
                    computerView.getFileSystem().setCWD(path);
                    ctx.html(render("templates/editors.html"));

                    //ctx.html(computerView.getVisualShell());
                    // .getVisualShell() is outdated. renderFileTree() is manual input.
                })
                .get("/fs/open/*", ctx -> {
                    String path = ctx.path().substring("/fs/open/".length());
                    boolean successfullyOpened = computerView.openFile(path);
                    if (!successfullyOpened) {
                        ctx.status(HttpStatus.NOT_FOUND);
                    }
                    ctx.html(render("templates/editors.html"));
                })
                .post("/fs/cwd", ctx -> {
                    computerView.getFileSystem().setCWD(ctx.formParam("cwd"));
                    ctx.html(render("templates/editors.html"));

                })
                .get("/fs/close", ctx -> {
                    computerView.closeFile();
                    ctx.html(render("templates/editors.html"));
                })
                .sse("/sse", client -> {
                    client.keepAlive();
                    client.onClose(() -> sseClients.remove(client));
                    sseClients.add(client);
                });

        // start server
        javalinApp.start(PORT);
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
