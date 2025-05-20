package mtmc.os.shell.builtins;

import mtmc.emulator.MonTanaMiniComputer;
import mtmc.os.shell.ShellCommand;
import mtmc.os.utils.ImageUtils;
import mtmc.tokenizer.MTMCTokenizer;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
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
                    Iterable<Integer> rows = computer.getDisplay().getRows();
                    Iterable<Integer> cols = computer.getDisplay().getColumns();
                    for (Integer row : rows) {
                        for (Integer col : cols) {
                            computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), (short) random.nextInt(0, 4));
                        }
                    }
                }
                case "reset" -> {
                    Iterable<Integer> rows = computer.getDisplay().getRows();
                    Iterable<Integer> cols = computer.getDisplay().getColumns();
                    for (Integer row : rows) {
                        for (Integer col : cols) {
                            computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), (short) 0);
                        }
                    }
                }
                case "invert" -> {
                    Iterable<Integer> rows = computer.getDisplay().getRows();
                    Iterable<Integer> cols = computer.getDisplay().getColumns();
                    for (Integer row : rows) {
                        for (Integer col : cols) {
                            short inverted = (short) (3 - computer.getDisplay().getValueFor(row.shortValue(), col.shortValue()));
                            computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), inverted);
                        }
                    }
                }
                case "image" -> {
                    if (tokens.more()) {
                        String imagePath = tokens.collapseTokensAsString();
                        File file = computer.getOS().loadFile(imagePath);
                        BufferedImage img = ImageIO.read(file);
                        Dimension scaleDimensions = ImageUtils.getScaledDimension(img, 128, 128);
                        BufferedImage scaledImage = ImageUtils.scaleImage(img, scaleDimensions);
                        int xpad = (128 - scaledImage.getWidth()) / 2;
                        int ypad = (128 - scaledImage.getHeight()) / 2;
                        for (int x = 0; x < scaledImage.getWidth(); x++) {
                            for (int y = 0; y < scaledImage.getHeight(); y++) {
                                int rgb = scaledImage.getRGB(x, y);
                                int alpha = (rgb >> 24) & 0xff;
                                if (alpha > 128) {
                                    int twoBitValue = ImageUtils.findClosestDisplayColor(rgb);
                                    computer.getDisplay().setValueFor((short) (x + xpad), (short) (y + ypad), (short) twoBitValue);
                                }
                            }
                        }
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
            computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), color.shortValue());
        } else {
            usageException();
        }
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
