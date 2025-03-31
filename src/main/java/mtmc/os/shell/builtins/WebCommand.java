package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;
import mtmc.web.WebServer;

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
