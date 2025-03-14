package edu.montana.cs.mtmc.web;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import io.javalin.Javalin;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class WebServer {
    public static final int PORT = 8080;
    public static final String SERVER_URL = "http://localhost:" + PORT;
    private static WebServer instance;
    private final MonTanaMiniComputer computer;
    private Javalin javalinApp;

    PebbleEngine templateEngine = new PebbleEngine.Builder().cacheActive(false).build();

    public WebServer(MonTanaMiniComputer computer) {
        this.computer = computer;
        initRoutes();
    }

    private void initRoutes() {
        // config
        this.javalinApp = Javalin.create(javalinConfig -> {
            javalinConfig.staticFiles.add("public");
        });

        // paths
        javalinApp.get("/", ctx -> ctx.html(render("index.html",
                "computer", computer,
                "display", computer.getDisplay())));

        // start server
        javalinApp.start(PORT);
    }

    private String render(String templateName, Object... args) throws IOException {
        PebbleTemplate template = templateEngine.getTemplate("templates/" + templateName);
        HashMap<String, Object> context = new HashMap<>();
        for (int i = 0; i < args.length; i = i + 2) {
            Object name = args[i];
            Object value = i + 1 < args.length ? args[i + 1] : null;
            context.put(String.valueOf(name), value);
        }
        Writer writer = new StringWriter();
        template.evaluate(writer, context);
        String string = writer.toString();
        return string;
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
