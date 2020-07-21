package info.ginj.ui.component;

import javax.swing.*;

/**
 * Standard label, but yellow on a rounded dark rectangle
 */
public class BorderedLabel extends JLabel {
    public BorderedLabel(String text) {
        this(text, null, LEADING);
    }

    public BorderedLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    public BorderedLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    public BorderedLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        setName("BorderedLabel"); // To be addressed in synth.xml
    }
}
