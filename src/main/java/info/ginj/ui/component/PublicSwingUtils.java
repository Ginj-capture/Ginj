package info.ginj.ui.component;

import javax.swing.*;
import java.awt.*;

public class PublicSwingUtils {
    /**
     * Returns true if the Icon <code>icon</code> is an instance of
     * ImageIcon, and the image it contains is the same as <code>image</code>.
     *
     * copied from javax.swing.SwingUtilities because this method is not public there (??)
     */
    public static boolean doesIconReferenceImage(Icon icon, Image image) {
        Image iconImage = (icon != null && (icon instanceof ImageIcon)) ?
                ((ImageIcon)icon).getImage() : null;
        return (iconImage == image);
    }


    /*
     * Convenience function for determining ComponentOrientation.  Helps us
     * avoid having Munge directives throughout the code.
     *
     * Copied from javax.swing.plaf.basic.BasicGraphicsUtils because this method is not public there (??)
     * Btw also exists javax.swing.SwingUtilities but also not public...
     */
    public static boolean isLeftToRight( Component c ) {
        return c.getComponentOrientation().isLeftToRight();
    }
}
