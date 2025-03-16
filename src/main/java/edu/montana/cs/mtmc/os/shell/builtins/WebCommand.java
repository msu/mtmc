package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;
import edu.montana.cs.mtmc.web.WebServer;

import java.awt.*;

public class WebCommand extends ShellCommand {
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception{
        WebServer server = WebServer.getInstance(computer);
        Desktop.getDesktop().browse(server.getURL());
    }

    @Override
    public String getHelp() {
        return "web - starts the web UI";
    }
}
