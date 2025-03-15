package edu.montana.cs.mtmc.os.shell;

import edu.montana.cs.mtmc.emulator.MonTanaMiniComputer;
import edu.montana.cs.mtmc.os.utils.ImageUtils;
import edu.montana.cs.mtmc.tokenizer.MTMCTokenizer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.IDENTIFIER;
import static edu.montana.cs.mtmc.tokenizer.MTMCToken.TokenType.INTEGER;

public class DisplayCommand extends ShellCommand {

    Random random = new Random();

    @Override
    void exec(MTMCTokenizer tokens, MonTanaMiniComputer computer) throws Exception {
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
                        File file = new File("disk/" + imagePath);
                        BufferedImage img = ImageIO.read(file);
                        Dimension scaleDimensions = ImageUtils.getScaledDimension(img, 64, 64);
                        BufferedImage scaledImage = ImageUtils.scaleImage(img, scaleDimensions);
                        int xpad = (64 - scaledImage.getWidth()) / 2;
                        int ypad = (64 - scaledImage.getHeight()) / 2;
                        for (int x = 0; x < scaledImage.getWidth(); x++) {
                            for (int y = 0; y < scaledImage.getHeight(); y++) {
                                int rgb = scaledImage.getRGB(x, y);
                                int twoBitValue = ImageUtils.findClosestDisplayColor(rgb);
                                computer.getDisplay().setValueFor((short) (x + xpad), (short) (y + ypad), (short) twoBitValue);
                            }
                        }
                    }
                }
                default -> usageException();
            }
        } else if (tokens.match(INTEGER)) {
            Integer row = tokens.consumeAsInteger();
            Integer col = tokens.require(INTEGER, this::usageException).getIntegerValue();
            Integer color = tokens.require(INTEGER, this::usageException).getIntegerValue();
            computer.getDisplay().setValueFor(row.shortValue(), col.shortValue(), color.shortValue());
        } else {
            usageException();
        }
    }

    @Override
    public String getHelp() {
        return """
display [options] - updates the display
  fuzz              - displays random colors
  reset             - resets the display
  invert            - inverts the display
  image <file>      - loads the given image into the display
  <x> <y> <color>   - sets the given pixel to the given color [0-3]""";
    }

}
