package mtmc.os.shell.builtins;

import java.util.Arrays;
import java.util.List;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;

import static mtmc.tokenizer.MTMCToken.TokenType.IDENTIFIER;
import static mtmc.tokenizer.MTMCToken.TokenType.INTEGER;

public class SpeedCommand extends ShellCommand {
    
    private List<Integer> speeds = Arrays.asList(new Integer[] {
        1, 10, 100, 1000, 10000, 100000, 1000000
    });
    
    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        if (tokens.match(IDENTIFIER)) {
            computer.setSpeed(0);
        } else if (tokens.match(INTEGER)) {
            Integer speed = tokens.consumeAsInteger();
            if (!speeds.contains(speed)) {
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
