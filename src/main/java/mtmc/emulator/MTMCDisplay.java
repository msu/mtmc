package mtmc.emulator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class MTMCDisplay {

    public static final int ROWS = 144;
    public static final int COLS = 160;
    public static final String SPLASH_SCREEN = "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";
    private BufferedImage buffer = new BufferedImage(COLS, ROWS, BufferedImage.TYPE_INT_ARGB);
    private DisplayColor currentColor;
    private BufferedImage[] graphics;
    private byte[] byteArray;

    public void setColor(short registerValue) {
        currentColor = DisplayColor.values()[Math.min(registerValue, 3)];
    }

    public enum DisplayColor {
        DARK(42, 69, 59),
        MEDIUM(54, 93, 72),
        LIGHT(87, 124, 68),
        LIGHTEST(127, 134, 15);
        final int r, g, b, intVal;
        final private Color javaColor;

        DisplayColor(int r, int g, int b){
            this.r = r;
            this.g = g;
            this.b = b;
            this.intVal = 0xFF << 24 | r << 16 | g << 8 | b;
            javaColor = new Color(r, g, b);
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

        public Color getJavaColor() {
            return javaColor;
        }
    }

    private final MonTanaMiniComputer computer;

    public MTMCDisplay(MonTanaMiniComputer computer) {
        this.computer = computer;
        reset();
        loadSplashScreen();
        sync();
    }

    private void loadSplashScreen() {
        try {
            byte[] bytes = Base64.getDecoder().decode(SPLASH_SCREEN);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage img = null;
            img = ImageIO.read(bais);
            loadScaledImage(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private BufferedImage loadImage(byte[] data) {
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch(IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
    
    public void loadGraphics(byte[][] data) {
        if(data == null) {
            graphics = new BufferedImage[0];
            return;
        }
        
        graphics = new BufferedImage[data.length];
        for (int i=0; i<data.length; i++) {
            graphics[i] = loadImage(data[i]);
        }
    }

    public void reset() {
        currentColor = DisplayColor.DARK;
        for(int col = 0; col < COLS; col++ ) {
            for(int row = 0; row < ROWS; row++ ) {
                setPixel(col, row, DisplayColor.LIGHTEST);
            }
        }
    }
    
    public boolean hasGraphic(int index) {
        return (graphics != null && index >= 0  && index < graphics.length);
    }

    public void setPixel(int col, int row, int value) {
        DisplayColor color = DisplayColor.values()[value];
        setPixel(col, row, color);
    }

    public void setPixel(int col, int row, DisplayColor color) {
        buffer.setRGB(col, row, color.intVal);
    }

    public short getPixel(int col, int row) {
        int rgb = buffer.getRGB(col, row);
        return DisplayColor.indexFromInt(rgb);
    }

    public void drawLine(short startCol, short startRow, short endCol, short endRow) {
        Graphics graphics = buffer.getGraphics();
        graphics.setColor(currentColor.getJavaColor());
        graphics.drawLine(startCol, startRow, endCol, endRow);
        graphics.dispose();
    }

    public void drawRectangle(short startCol, short startRow, short width, short height) {
        Graphics graphics = buffer.getGraphics();
        graphics.setColor(currentColor.getJavaColor());
        graphics.fillRect(startCol, startRow, width, height);
        graphics.dispose();
    }
    
    public void drawImage(int image, int x, int y) {
        BufferedImage graphic = graphics[image];
        Graphics graphics = buffer.getGraphics();
        graphics.drawImage(graphic, x, y, null);
        graphics.dispose();
    }
    
    public void drawImage(int image, int x, int y, int width, int height) {
        BufferedImage graphic = graphics[image];
        Graphics graphics = buffer.getGraphics();
        graphics.drawImage(graphic, x, y, width, height, null);
        graphics.dispose();
    }
    
    public void drawImage(int image, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        BufferedImage graphic = graphics[image];
        Graphics graphics = buffer.getGraphics();
        graphics.drawImage(graphic, dx, dy, dx+dw, dy+dh, sx, sy, sx+sw, sy+sh, null);
        graphics.dispose();
    }

    public void sync() {
        var baos = new ByteArrayOutputStream();
        try {
            buffer.flush();
            ImageIO.write(buffer, "png", baos);
            byteArray = baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        computer.notifyOfDisplayUpdate();
    }

    public byte[] toPng() {
        return byteArray;
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
                if (alpha > 0xFF/2) {
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
    
    public static BufferedImage convertImage(BufferedImage image) {
        var arbg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                if (alpha > 0xFF/2) {
                    DisplayColor displayColor = findClosestColor(rgb);
                    arbg.setRGB(x, y, displayColor.intVal);
                }
            }
        }
        return arbg;
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
