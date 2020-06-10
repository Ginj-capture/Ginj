package info.ginj.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.net.URL;

public class Util {
    public static final Color AREA_SELECTION_COLOR = new Color(251, 185, 1);
    public static final Color SELECTION_SIZE_BOX_COLOR = new Color(0, 0, 0, 128);
    public static final Color UNSELECTED_AREA_DIMMED_COLOR = new Color(144, 144, 144, 112);

    public static final Color WINDOW_BACKGROUND_COLOR = new Color(37, 38, 40);
    public static final Color ICON_ENABLED_COLOR = new Color(243,205,77);
    public static final Color TOOLBAR_ICON_ENABLED_COLOR = new Color(238,179,8);
    public static final Color HELP_FOREGROUND_COLOR = Color.WHITE;
    public static final Color TEXT_TOOL_DEFAULT_FOREGROUND_COLOR = Color.BLACK;

    public static final Color HANDLE_CENTER_COLOR = new Color(72, 72, 212, 128);
    public static final Color HANDLE_GREY_1_COLOR = new Color(253, 253, 253, 128);
    public static final Color HANDLE_GREY_2_COLOR = new Color(242, 242, 242, 128);
    public static final Color HANDLE_GREY_3_COLOR = new Color(180, 180, 180, 128);
    public static final Color HANDLE_GREY_4_COLOR = new Color(136, 136, 136, 128);

    /**
     * Lay out components of a Panel and compute its size, like pack() for a Window.
     * This method computes the size of the given panel by adding it to a temporary window.
     * Warning, must be called before adding the panel to its final parent, because it will be removed from it otherwise
     * @return the size of the panel when packed
     */
    public static Dimension packPanel(JPanel panel) {
        JWindow window = new JWindow();
        window.setLayout(new BorderLayout());
        window.getContentPane().add(panel);
        window.pack();
        final Dimension size = window.getSize();
        panel.setSize(size);
        return size;
    }

    /**
     * Create an ImageIcon of the given width x height from an image loaded from the given URL
     * @param resource the URL of the source image
     * @param width the desired width
     * @param height the desired height
     * @return the scaled ImageIcon
     */
    public static ImageIcon createIcon(URL resource, int width, int height) {
        return createIcon(resource, width, height, null);
    }

    /**
     * Create an ImageIcon of the given width x height and with the given base color from an image loaded from the given URL
     * @param resource the URL of the source image
     * @param width the desired width
     * @param height the desired height
     * @param color the desired base color to shift this image to
     * @see #tint(BufferedImage, Color) for more info about color shift
     * @return the scaled ImageIcon
     */
    public static ImageIcon createIcon(URL resource, int width, int height, Color color) {
        try {
            BufferedImage image = ImageIO.read(resource);
            if (color != null) {
                image = tint(image, color);
            }
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }
        catch (IOException e) {
            System.err.println("Error loading resource: " + resource);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Apply the given color shift to the given image.
     * Basically, the given color will replace black in the source image, and all greyscale levels in the source will be be mapped between this color and white
     * @param source the source image
     * @param color the base color to shift to
     * @return the colored image
     */
    public static BufferedImage tint(BufferedImage source, Color color) {
        final BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final int minRed = color.getRed();
        final int minGreen = color.getGreen();
        final int minBlue = color.getBlue();

        final double redFactor = (255 - minRed) / 255.0;
        final double greenFactor = (255 - minGreen) / 255.0;
        final double blueFactor = (255 - minBlue) / 255.0;

        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                Color pixelColor = new Color(source.getRGB(x, y), true);
                int r = (int) (minRed + (pixelColor.getRed() * redFactor));
                int g = (int) (minGreen + (pixelColor.getGreen() * greenFactor));
                int b = (int) (minBlue + (pixelColor.getBlue() * blueFactor));
                int a = pixelColor.getAlpha();

                result.setRGB(x, y, new Color(r,g,b,a).getRGB());
            }
        }
        return result;
    }

    /**
     * Convert an image to greyscale and apply a dim effect to it.
     * Used to show unselected area when drawing selection
     * @param image the source image
     * @return the dimmed result
     */
    public static Image makeDimmedImage(BufferedImage image) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        final BufferedImage greyScale = op.filter(image, null);
        final Graphics graphics = greyScale.getGraphics();
        graphics.setColor(UNSELECTED_AREA_DIMMED_COLOR);
        graphics.fillRect(0,0,image.getWidth(), image.getHeight());
        graphics.dispose();
        return greyScale;
    }

    /**
     * Make a color translucent, that is the same RGB but with half opacity
     * @param color the source color
     * @return the translucent color
     */
    public static Color getTranslucentColor(Color color) {
        // Make 50% opacity
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
    }
}
