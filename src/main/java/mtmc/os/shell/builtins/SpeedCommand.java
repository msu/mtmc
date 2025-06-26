package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

import static mtmc.tokenizer.MTMCToken.TokenType.IDENTIFIER;
import static mtmc.tokenizer.MTMCToken.TokenType.INTEGER;

public class SpeedCommand extends ShellCommand {
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
