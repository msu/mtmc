package edu.montana.cs.mtmc.os.utils;

import edu.montana.cs.mtmc.emulator.MTMCDisplay;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public static final Color3i DARK_COLOR = new Color3i(MTMCDisplay.DARK);
    public static final Color3i MEDIUM_COLOR = new Color3i(MTMCDisplay.MEDIUM);
    public static final Color3i LIGHT_COLOR = new Color3i(MTMCDisplay.LIGHT);
    public static final Color3i WHITE_COLOR = new Color3i(MTMCDisplay.WHITE);
    private static final java.util.List<Color3i> PALETTE = java.util.List.of(DARK_COLOR, MEDIUM_COLOR, LIGHT_COLOR, WHITE_COLOR);

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

    public static int findClosestDisplayColor(int color) {
        Color3i closestPaletteColor = findClosestPaletteColor(color);
        if (closestPaletteColor == DARK_COLOR) {
            return 0;
        } else if(closestPaletteColor == MEDIUM_COLOR) {
            return 1;
        } else if(closestPaletteColor == LIGHT_COLOR) {
            return 2;
        } else {
            return 3;
        }
    }

    public static BufferedImage dithering(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Color3i[][] buffer = new Color3i[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = new Color3i(image.getRGB(x, y));
            }
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color3i old = buffer[y][x];
                Color3i nem = findClosestPaletteColor(old.toRGB());
                image.setRGB(x, y, nem.toColor().getRGB());

                Color3i error = old.sub(nem);

                if (x + 1 < width) buffer[y][x + 1] = buffer[y][x + 1].add(error.mul(7. / 16));
                if (x - 1 >= 0 && y + 1 < height) buffer[y + 1][x - 1] = buffer[y + 1][x - 1].add(error.mul(3. / 16));
                if (y + 1 < height) buffer[y + 1][x] = buffer[y + 1][x].add(error.mul(5. / 16));
                if (x + 1 < width && y + 1 < height)
                    buffer[y + 1][x + 1] = buffer[y + 1][x + 1].add(error.mul(1. / 16));
            }
        }

        return image;
    }

    private static Color3i findClosestPaletteColor(int colorInt) {
        Color3i match = new Color3i(colorInt);
        Color3i closest = PALETTE.get(0);
        for (Color3i color : PALETTE) {
            if (color.diff(match) < closest.diff(match)) {
                closest = color;
            }
        }
        return closest;
    }

    static class Color3i {

        private int r, g, b;

        public Color3i(String s) {
            this(Integer.parseInt(s, 16));
        }

        public Color3i(int c) {
            Color color = new Color(c);
            this.r = color.getRed();
            this.g = color.getGreen();
            this.b = color.getBlue();
        }

        public Color3i(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Color3i add(Color3i o) {
            return new Color3i(r + o.r, g + o.g, b + o.b);
        }

        public Color3i sub(Color3i o) {
            return new Color3i(r - o.r, g - o.g, b - o.b);
        }

        public Color3i mul(double d) {
            return new Color3i((int) (d * r), (int) (d * g), (int) (d * b));
        }

        public int diff(Color3i o) {
            int Rdiff = o.r - r;
            int Gdiff = o.g - g;
            int Bdiff = o.b - b;
            int distanceSquared = Rdiff * Rdiff + Gdiff * Gdiff + Bdiff * Bdiff;
            return distanceSquared;
        }

        public int toRGB() {
            return toColor().getRGB();
        }

        public Color toColor() {
            return new Color(clamp(r), clamp(g), clamp(b));
        }

        public int clamp(int c) {
            return Math.max(0, Math.min(255, c));
        }
    }
}
