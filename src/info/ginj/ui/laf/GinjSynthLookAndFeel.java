package info.ginj.ui.laf;

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

public class GinjSynthLookAndFeel extends SynthLookAndFeel {
    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        if (icon instanceof ImageIcon) {
            return new ImageIcon(createDisabledImage(((ImageIcon)icon).getImage()));
        }
        return null;
    }

    private static Image createDisabledImage(Image i) {
        ImageProducer prod = new FilteredImageSource(i.getSource(), new RGBImageFilter() {
            public int filterRGB(int x, int y, int rgb) {
                // extract alpha mask
                int alphamask = rgb & 0xFF000000;

                // convert to HSB
                float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, null);
                // desaturate (half saturation)
                hsb[1] *= 0.5;
                // dim (half brightness)
                hsb[2] *= 0.5;
                // convert back to RGB
                int rgbval = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);

                // reapply alpha
                rgbval = rgbval & 0x00FFFFFF | alphamask;
                return rgbval;
            }
        });
        return Toolkit.getDefaultToolkit().createImage(prod);
    }

}
