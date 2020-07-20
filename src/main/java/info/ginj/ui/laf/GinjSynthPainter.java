package info.ginj.ui.laf;

import com.easynth.designer.laf.painter.EaSynthPainter;
import sun.awt.AppContext;
import sun.swing.plaf.synth.Paint9Painter;

import javax.swing.*;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

/**
 * Generally speaking, this is a pass through for EaSynthPainter.
 * Some methods can be overridden here though.
 */
public class GinjSynthPainter extends EaSynthPainter {

    // Copied from private javax.swing.plaf.synth.ImagePainter
    private static final StringBuffer CACHE_KEY = new StringBuffer("GinjCacheKey");
    private final Paint9Painter imageCache;
    private static Paint9Painter getPaint9Painter() {
        // A SynthPainter is created per <imagePainter>.  We want the
        // cache to be shared by all, and we don't use a static because we
        // don't want it to persist between look and feels.  For that reason
        // we use a AppContext specific Paint9Painter.  It's backed via
        // a WeakRef so that it can go away if the look and feel changes.
        synchronized(CACHE_KEY) {
            @SuppressWarnings("unchecked")
            WeakReference<Paint9Painter> cacheRef =
                    (WeakReference<Paint9Painter>) AppContext.getAppContext().
                            get(CACHE_KEY);
            Paint9Painter painter;
            if (cacheRef == null || (painter = cacheRef.get()) == null) {
                painter = new Paint9Painter(5);
                cacheRef = new WeakReference<>(painter);
                AppContext.getAppContext().put(CACHE_KEY, cacheRef);
            }
            return painter;
        }
    }

    public GinjSynthPainter() {
        this.imageCache = getPaint9Painter();
    }

    /**
     * Paint the progress bar foreground, that consist of progress bar filling exactly the background (except insets) and the indication image.
     * This is customized compared to EaSynth that requires an indication image and only draws a thin horizontal line
     *
     * @param context
     * 		the SynthContext object for painting
     * @param g
     * 		the Graphics object to paint
     * @param x
     * 		the x position of the rectangle to paint
     * @param y
     * 		the y position of the rectangle to paint
     * @param w
     * 		the width of the rectangle to paint
     * @param h
     * 		the height of the rectangle to paint
     * @param orientation
     * 		the orientation of the progressbar
     */
    @Override
    public void paintProgressBarForeground(final SynthContext context, final Graphics g,
                                               final int x, final int y, final int w, final int h, final int orientation) {
        final Graphics2D g2 = (Graphics2D) g.create();
        final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.progressbar.indication.image");
        final JProgressBar progressBar = (JProgressBar) context.getComponent();
        final boolean isDisabled = (context.getComponentState() & SynthConstants.DISABLED) != 0;
        final UIDefaults uiDefaults = UIManager.getDefaults();

        // Size of the component (assuming horizontal orientation)
        final Dimension size = (Dimension) context.getStyle().get(context, "ProgressBar.horizontalSize");

        int rectX;
        int rectY;
        int rectHeight;
        Insets bgInsets = (Insets)context.getStyle().get(context, "EaSynth.progressbar.bg.insets");
        if (bgInsets == null) {
            bgInsets = new Insets(0, 0, 0, 0);
        }

        rectX = bgInsets.left;
        rectY = bgInsets.top;
        rectHeight = size.height - bgInsets.top - bgInsets.bottom;

        Color lineColor = uiDefaults.getColor("EaSynth.progressbar.line.color");
        if (lineColor == null) {
            lineColor = new Color(255, 255, 255, 130);
        }
        if (w > 0 && h > 0) {
            int imgWidth = 0;
            int imgHeight = 0;
            if (icon != null) {
                imgWidth = icon.getIconWidth();
                imgHeight = icon.getIconHeight();
            }
            if (JProgressBar.HORIZONTAL == orientation) {
                if (!progressBar.isIndeterminate() && w >= imgWidth) {
                    g2.setColor(lineColor);
                    // draw the progress rectangle
                    g2.fillRect(rectX + imgWidth / 2, rectY, w, rectHeight);
                }

                if (!isDisabled) {
                    // draw the indication
                    int destPosX = x + w - imgWidth;
                    if (progressBar.isIndeterminate()) {
                        // need to re-calculate the x position
                        final int midRange = progressBar.getWidth() - w;
                        final int curMid = imgWidth / 2 + (progressBar.getWidth() - imgWidth) * x / midRange;
                        destPosX = curMid - imgWidth / 2;
                    }
                    if (destPosX >= 0 && icon != null) {
                        final int destPosY = y + (h - imgHeight) / 2;
                        g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
                    }
                }
            } else {
                if (!progressBar.isIndeterminate() && h >= imgHeight) {
                    // draw the progress bar
                    g2.drawLine((x + w) / 2, y + imgHeight / 2, (x + w) / 2, y + h - imgHeight / 2);
                }

                if (!isDisabled) {
                    // draw the indication
                    final int destPosX = x + (w - imgWidth) / 2;
                    int destPosY = y;
                    if (progressBar.isIndeterminate()) {
                        // need to re-calculate the y position
                        final int midRange = progressBar.getHeight() - h;
                        final int curMid = imgHeight / 2 + (progressBar.getHeight() - imgHeight) * y / midRange;
                        destPosY = curMid - imgHeight / 2;
                    }
                    if( icon != null) {
                        g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight, 0, 0, imgWidth, imgHeight, null);
                    }
                }
            }
        }
    }

    /**
     * Paint the thumb background for scroll bar.
     *
     * @param context
     * 		the SynthContext object for painting
     * @param g
     * 		the Graphics object to paint
     * @param x
     * 		the x position of the rectangle to paint
     * @param y
     * 		the y position of the rectangle to paint
     * @param w
     * 		the width of the rectangle to paint
     * @param h
     * 		the height of the rectangle to paint
     * @param orientation
     * 		the orientation of the scroll bar
     */
    public void paintScrollBarThumbBackground(final SynthContext context, final Graphics g,
                                              final int x, final int y, final int w, final int h, final int orientation) {
        final JScrollBar scrollBar = (JScrollBar)context.getComponent();
        final boolean isVertical = scrollBar.getOrientation() == JScrollBar.VERTICAL;

        final ImageIcon bgIcon = (ImageIcon) context.getStyle().getIcon(context, "Ginj.scrollbar.thumb.horizontal.background");
        final Insets insets = (Insets) context.getStyle().get(context, "Ginj.scrollbar.thumb.bg.insets");

        if (bgIcon != null) {
            if (isVertical) {
                imageCache.paint(context.getComponent(), g, x, y, w, h, bgIcon.getImage(), insets, insets, Paint9Painter.PaintType.PAINT9_STRETCH, Paint9Painter.PAINT_ALL);
            }
            else {
                imageCache.paint(context.getComponent(), g, x, y, w, h, rotate90(toBufferedImage(bgIcon.getImage())), insets, insets, Paint9Painter.PaintType.PAINT9_STRETCH, Paint9Painter.PAINT_ALL);
            }
        }
    }


    /////////////////
    // Utils

    public BufferedImage rotate90(BufferedImage image) {
        BufferedImage rotated = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                //noinspection SuspiciousNameCombination
                rotated.setRGB(image.getHeight() - y - 1, x, image.getRGB(x, y));
            }
        }
        return rotated;
    }

    // From https://stackoverflow.com/a/13605411/13551878
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

}
