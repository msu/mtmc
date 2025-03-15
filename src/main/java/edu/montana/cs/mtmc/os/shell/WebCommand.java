package edu.montana.cs.mtmc.os.shell;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;
import edu.montana.cs.mtmc.web.WebServer;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

public class WebCommand extends ShellCommand {
    @Override
    void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception{
        WebServer server = WebServer.getInstance(computer);
        Desktop.getDesktop().browse(server.getURL());
    }

    @Override
    public String getHelp() {
        return "web - starts the web UI";
    }
}
