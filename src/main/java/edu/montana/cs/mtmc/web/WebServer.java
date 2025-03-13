package edu.montana.cs.mtmc.web;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import io.javalin.Javalin;

import java.net.URI;
import java.net.URISyntaxException;

public class WebServer {
    public static final int PORT = 8080;
    public static final String SERVER_URL = "http://localhost:" + PORT;
    private static WebServer instance;
    private final MonTanaMiniComputer computer;
    private Javalin javalinApp;

    public WebServer(MonTanaMiniComputer computer) {
        this.computer = computer;
        initRoutes();
    }

    private void initRoutes() {
        this.javalinApp = Javalin.create(/*config*/)
                .get("/", ctx -> ctx.result("Hello World"))
                .start(PORT);
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
}
