package info.ginj.ui.component;

import javax.swing.*;

/**
 * Large (non-toggle) button in left tool bar, for color selection
 */
public class ToolButton extends JButton {
    public ToolButton() {
        this(null, null);
    }

    public ToolButton(Icon icon) {
        this(null, icon);
    }

    public ToolButton(String text, Icon icon) {
        super(text, icon);
        setName("ToolButton"); // To be addressed in synth.xml
    }
}
