package info.ginj.ui.component;

import javax.swing.*;

/**
 * Small buttons in left tool bar, for Undo/Redo
 */
public class MiniToolButton extends JButton {
    public MiniToolButton() {
        this(null, null);
    }

    public MiniToolButton(Icon icon) {
        this(null, icon);
    }

    public MiniToolButton(String text, Icon icon) {
        super(text, icon);
        setName("MiniToolButton"); // To be addressed in synth.xml
    }
}
