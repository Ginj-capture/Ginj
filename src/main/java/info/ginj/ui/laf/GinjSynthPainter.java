package info.ginj.ui.laf;

import com.easynth.designer.laf.painter.EaSynthPainter;

import javax.swing.*;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import java.awt.*;

/**
 * Generally speaking, this is a pass through for EaSynthPainter.
 * Some methods can be overridden here though.
 */
public class GinjSynthPainter extends EaSynthPainter {

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
        int rectWidth;
        Insets bgInsets = (Insets)context.getStyle().get(context, "EaSynth.progressbar.bg.insets");
        if (bgInsets == null) {
            bgInsets = new Insets(0, 0, 0, 0);
        }

        rectX = bgInsets.left;
        rectY = bgInsets.top;
        rectHeight = size.height - bgInsets.top - bgInsets.bottom;
        rectWidth = ((size.width - bgInsets.left - bgInsets.right) * w) / size.width;

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
                    //g2.drawLine(x + imgWidth / 2, (y + h) / 2, x + w - imgWidth / 2, (y + h) / 2);
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


}
