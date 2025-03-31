package edu.montana.cs.mtmc.os.shell.builtins;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.shell.ShellCommand;
import edu.montana.cs.mtmc.os.utils.ImageUtils;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.IDENTIFIER;
import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.INTEGER;

public class SpeedCommand extends ShellCommand {

    Random random = new Random();

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        if (tokens.match(IDENTIFIER)) {
            computer.setSpeed(-1);
        } else if (tokens.match(INTEGER)) {
            Integer speed = tokens.consumeAsInteger();
            if (speed != 1 && speed != 10 && speed != 100 && speed != 1000) {
                usageException();
            }
            computer.setSpeed(speed);
        } else {
            usageException();
        }
    }

    @Override
    public String getHelp() {
        return """
speed <val> - set the speed of the computer
  where <val> is one of:
     raw  - run with no simulated speed delay
     1    - run the computer at 1hz
     10   - run the computer at 10hz
     100  - run the computer at 100hz
     1000 - run the computer at 1khz""";
    }

}
