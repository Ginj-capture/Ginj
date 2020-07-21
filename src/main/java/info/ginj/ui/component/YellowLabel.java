package info.ginj.ui.component;

import javax.swing.*;

/**
 * Standard label, but yellow
 */
public class YellowLabel extends JLabel {
    public YellowLabel(String text) {
        this(text, null, LEADING);
    }

    public YellowLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    public YellowLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    public YellowLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        setName("YellowLabel"); // To be addressed in synth.xml
    }
}
