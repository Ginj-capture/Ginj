package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Toggle buttons in the color selection popup
 */
public class GinjColorToggleButton extends JToggleButton {

    private static final int COLOR_BUTTON_ICON_WIDTH = 16;
    private static final int COLOR_BUTTON_ICON_HEIGHT = 16;

    private Color color;

    public GinjColorToggleButton(Color color) {
        this(null, null, color, false);
        this.color = color;
    }

    public GinjColorToggleButton(Icon icon) {
        this(null, icon, null, false);
    }

    public GinjColorToggleButton(Icon icon, boolean selected) {
        this(null, icon, null, selected);
    }

    public GinjColorToggleButton(String text, Icon icon, Color color, boolean selected) {
        super(text, icon, selected);
        if (color != null) {
            setIcon(Util.createRectColorIcon(color, COLOR_BUTTON_ICON_WIDTH, COLOR_BUTTON_ICON_HEIGHT));
        }
        setName("GinjColorToggleButton"); // To be addressed in laf.xml
    }

    public Color getColor() {
        return color;
    }
}
