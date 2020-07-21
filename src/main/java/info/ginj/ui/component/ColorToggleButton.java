package info.ginj.ui.component;

import info.ginj.util.UI;

import javax.swing.*;
import java.awt.*;

/**
 * Toggle buttons in the color selection popup
 */
public class ColorToggleButton extends JToggleButton {

    private static final int COLOR_BUTTON_ICON_WIDTH = 16;
    private static final int COLOR_BUTTON_ICON_HEIGHT = 16;

    private final Color color;

    public ColorToggleButton(Color color) {
        this(null, null, color, false);
    }

    public ColorToggleButton(Icon icon) {
        this(null, icon, null, false);
    }

    public ColorToggleButton(Icon icon, boolean selected) {
        this(null, icon, null, selected);
    }

    public ColorToggleButton(String text, Icon icon, Color color, boolean selected) {
        super(text, icon, selected);
        this.color = color;
        setName("ColorToggleButton"); // To be addressed in synth.xml
        if (color != null) {
            setIcon(UI.createRectColorIcon(color, COLOR_BUTTON_ICON_WIDTH, COLOR_BUTTON_ICON_HEIGHT));
        }
        else {
            setIcon(UI.createRectColorIcon(Color.BLACK, COLOR_BUTTON_ICON_WIDTH, COLOR_BUTTON_ICON_HEIGHT));
        }
    }

    public Color getColor() {
        return color;
    }
}
