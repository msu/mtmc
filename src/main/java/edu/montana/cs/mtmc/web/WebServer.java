package edu.montana.cs.mtmc.web;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebServer {

    public static final int PORT = 8080;
    public static final String SERVER_URL = "http://localhost:" + PORT;
    private static WebServer instance;

    private final MonTanaMiniComputer computer;
    private Javalin javalinApp;
    PebbleEngine templateEngine = new PebbleEngine.Builder().cacheActive(false).build();
    private final MTMCWebView computerView;
    Queue<SseClient> sseClients = new ConcurrentLinkedQueue<SseClient>();

    public WebServer(MonTanaMiniComputer computer) {
        this.computer = computer;
        this.computerView = new MTMCWebView(computer);
        initRoutes();
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
                .post("/cmd", ctx -> {
                    computer.getOS().processCommand(ctx.formParam("cmd"));
                    ctx.html("");
                    sseClients.forEach(c -> {
                        c.sendEvent("console", render("templates/display.html"));
                    });
                })
                .sse("/sse", client -> {
                    client.keepAlive();
                    client.onClose(()-> sseClients.remove(client));
                    sseClients.add(client);
                });;

        // start server
        javalinApp.start(PORT);
    }

    private String render(String templateName) {
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
        maybeInit(new MonTanaMiniComputer());
    }
}
