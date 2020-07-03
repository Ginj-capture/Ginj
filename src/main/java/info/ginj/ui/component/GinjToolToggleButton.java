package info.ginj.ui.component;

import javax.swing.*;

/**
 * Large toggle buttons in left tool bar, for overlays.
 */
public class GinjToolToggleButton extends JToggleButton {
    public GinjToolToggleButton() {
        this(null, null, false);
    }

    public GinjToolToggleButton(Icon icon) {
        this(null, icon, false);
    }

    public GinjToolToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public GinjToolToggleButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setName("GinjToolToggleButton"); // To be addressed in synth.xml
    }
}
