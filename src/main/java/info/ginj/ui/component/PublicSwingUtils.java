package info.ginj.ui.component;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.Objects;

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

    /**
     * Returns whether or not the scale used by {@code GraphicsConfiguration}
     * was changed.
     *
     * copied from sun.swing.SwingUtilities2 because sun.swing package is not visible (!)
     *
     * @param  ev a {@code PropertyChangeEvent}
     * @return whether or not the scale was changed
     * @since 11
     */
    public static boolean isScaleChanged(final PropertyChangeEvent ev) {
        return isScaleChanged(ev.getPropertyName(), ev.getOldValue(),
                ev.getNewValue());
    }

    /**
     * Returns whether or not the scale used by {@code GraphicsConfiguration}
     * was changed.
     *
     * copied from sun.swing.SwingUtilities2 because sun.swing package is not visible (!)
     *
     * @param  name the name of the property
     * @param  oldValue the old value of the property
     * @param  newValue the new value of the property
     * @return whether or not the scale was changed
     * @since 11
     */
    public static boolean isScaleChanged(final String name,
                                         final Object oldValue,
                                         final Object newValue) {
        if (oldValue == newValue || !"graphicsConfiguration".equals(name)) {
            return false;
        }
        var newGC = (GraphicsConfiguration) oldValue;
        var oldGC = (GraphicsConfiguration) newValue;
        var newTx = newGC != null ? newGC.getDefaultTransform() : null;
        var oldTx = oldGC != null ? oldGC.getDefaultTransform() : null;
        return !Objects.equals(newTx, oldTx);
    }

}
