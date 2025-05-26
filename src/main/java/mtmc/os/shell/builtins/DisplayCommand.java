package mtmc.os.shell.builtins;

import mtmc.emulator.MTMCDisplay;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.tokenizer.MTMCTokenizer;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static mtmc.tokenizer.MTMCToken.TokenType.IDENTIFIER;
import static mtmc.tokenizer.MTMCToken.TokenType.INTEGER;

public class DisplayCommand extends ShellCommand {

    Random random = new Random();

    @Override
    public void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
        if (tokens.match(IDENTIFIER)) {
            String option = tokens.consumeAsString();
            switch (option) {
                case "fuzz" -> {
                    for (int row = 0; row < MTMCDisplay.ROWS; row++) {
                        for (int col = 0; col < MTMCDisplay.COLS; col++) {
                            computer.getDisplay().setPixel(col, row, (short) random.nextInt(0, 4));
                        }
                    }
                }
                case "reset" -> {
                    computer.getDisplay().reset();
                }
                case "invert" -> {
                    for (int row = 0; row < MTMCDisplay.ROWS; row++) {
                        for (int col = 0; col < MTMCDisplay.COLS; col++) {
                            short color = computer.getDisplay().getPixel(col, row);
                            computer.getDisplay().setPixel(col, row, (short) 3 - color);
                        }
                    }
                }
                case "image" -> {
                    if (tokens.more()) {
                        String imagePath = tokens.collapseTokensAsString();
                        File file = computer.getOS().loadFile(imagePath);
                        BufferedImage img = ImageIO.read(file);
                        computer.getDisplay().loadScaledImage(img);
                    } else {
                        usageException();
                    }
                }
                default -> usageException();
            }
        } else if (tokens.match(INTEGER)) {
            Integer row = tokens.consumeAsInteger();
            Integer col = tokens.require(INTEGER, this::usageException).intValue();
            Integer color = tokens.require(INTEGER, this::usageException).intValue();
            computer.getDisplay().setPixel(row.shortValue(), col.shortValue(), color.shortValue());
        } else {
            usageException();
        }
        computer.getDisplay().sync();
    }

    @NotNull
    private static File loadFile(String imagePath) {
        File file = new File("disk/" + imagePath);
        return file;
    }

    @Override
    public String getHelp() {
        return """
disp [options] - updates the display
  fuzz              - displays random colors
  reset             - resets the display
  invert            - inverts the display
  image <file>      - loads the given image into the display
  <x> <y> <color>   - sets the given pixel to the given color [0-3]""";
    }

}
