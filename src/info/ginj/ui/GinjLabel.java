package info.ginj.ui;

import javax.swing.*;

/**
 * Standard label, but yellow
 */
public class GinjLabel extends JLabel {
    public GinjLabel(String text) {
        this(text, null, LEADING);
    }

    public GinjLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    public GinjLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    public GinjLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        setName("GinjLabel"); // To be addressed in laf.xml
    }
}
