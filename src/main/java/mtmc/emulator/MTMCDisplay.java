package mtmc.emulator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MTMCDisplay {

    public static final int ROWS = 140;
    public static final int COLS = 160;
    BufferedImage buffer = new BufferedImage(COLS, ROWS, BufferedImage.TYPE_INT_ARGB);

    public enum DisplayColor {
        DARK(42, 69, 59),
        MEDIUM(54, 93, 72),
        LIGHT(87, 124, 68),
        LIGHTEST(127, 134, 15);
        int r, g, b, intVal;
        DisplayColor(int r, int g, int b){
            this.r = r;
            this.g = g;
            this.b = b;
            this.intVal = 0xFF << 24 | r << 16 | g << 8 | b;
        }
        static short indexFromInt(int val) {
            if (val == LIGHTEST.intVal) {
                return 3;
            } else if(val == LIGHT.intVal) {
                return 2;
            } else if(val == MEDIUM.intVal) {
                return 1;
            } else  {
                return 0;
            }
        }
        public int distance(int r, int g, int b) {
            int dr = this.r - r;
            int dg = this.g - g;
            int db = this.b - b;
            int square = dr * dr + dg * dg + db * db;
            return square;
        }
        public String toRGBString() {
            return "rgb(" + this.r + "," + this.g + "," + this.b + ")";
        }
    }

    private final MonTanaMiniComputer computer;

    public MTMCDisplay(MonTanaMiniComputer computer) {
        this.computer = computer;
        reset();
    }

    public void reset() {
        for(int col = 0; col < COLS; col++ ) {
            for(int row = 0; row < ROWS; row++ ) {
                setPixel(col, row, DisplayColor.DARK);
            }
        }
    }

    public void setPixel(int col, int row, int value) {
        DisplayColor color = DisplayColor.values()[value];
        setPixel(col, row, color);
    }

    public void setPixel(int col, int row, DisplayColor color) {
        buffer.setRGB(col, row, color.intVal);
        computer.notifyOfDisplayUpdate();
    }

    public short getPixel(int col, int row) {
        int rgb = buffer.getRGB(col, row);
        return DisplayColor.indexFromInt(rgb);
    }

    public void drawLine(short startCol, short startRow, short endCol, short endRow) {
        Graphics graphics = buffer.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.drawLine(startCol, startRow, endCol, endRow);
    }

    public byte[] toPng() {
        var baos = new ByteArrayOutputStream();
        try {
            buffer.flush();
            ImageIO.write(buffer, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //=============================================
    // utilities
    //=============================================

    public void loadScaledImage(BufferedImage img) {
        Dimension scaleDimensions = getScaledDimension(img, COLS, ROWS);
        BufferedImage scaledImage = scaleImage(img, scaleDimensions);
        int xpad = (COLS - scaledImage.getWidth()) / 2;
        int ypad = (ROWS - scaledImage.getHeight()) / 2;
        for (int x = 0; x < scaledImage.getWidth(); x++) {
            for (int y = 0; y < scaledImage.getHeight(); y++) {
                int rgb = scaledImage.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                if (alpha > 128) {
                    DisplayColor displayColor = findClosestColor(rgb);
                    setPixel((short) (x + xpad), (short) (y + ypad), displayColor);
                }
            }
        }
    }

    public static Dimension getScaledDimension(BufferedImage image, int widthBound, int heightBound) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // first check if we need to scale width
        if (originalWidth > widthBound) {
            //scale width to fit
            newWidth = widthBound;
            //scale height to maintain aspect ratio
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        // then check if we need to scale even with the new height
        if (newHeight > heightBound) {
            //scale height to fit instead
            newHeight = heightBound;
            //scale width to maintain aspect ratio
            newWidth = (newHeight * originalWidth) / originalHeight;
        }
        return new Dimension(newWidth, newHeight);
    }

    public static BufferedImage scaleImage(BufferedImage original, Dimension scaleDimensions) {
        BufferedImage resized = new BufferedImage(scaleDimensions.width, scaleDimensions.height, original.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, scaleDimensions.width, scaleDimensions.height, 0, 0, original.getWidth(),
                original.getHeight(), null);
        g.dispose();
        return resized;
    }

    private static DisplayColor findClosestColor(int colorVal) {
        int r = colorVal >> 16 & 255;
        int g = colorVal >> 8 & 255;
        int b = colorVal & 255;
        DisplayColor closest = DisplayColor.DARK;
        for (DisplayColor color : DisplayColor.values()) {
            if (color.distance(r, g, b) < closest.distance(r, g, b)) {
                closest = color;
            }
        }
        return closest;
    }

}
