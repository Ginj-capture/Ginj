package info.ginj.ui.component;

import javax.swing.*;

/**
 * Large toggle buttons in left tool bar, for overlays.
 */
public class ToolToggleButton extends JToggleButton {
    public ToolToggleButton() {
        this(null, null, false);
    }

    public ToolToggleButton(Icon icon) {
        this(null, icon, false);
    }

    public ToolToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public ToolToggleButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setName("ToolToggleButton"); // To be addressed in synth.xml
    }
}
