package info.ginj.ui;

import javax.swing.*;

/**
 * Standard label, but yellow on a rounded dark rectangle
 */
public class GinjBorderedLabel extends JLabel {
    public GinjBorderedLabel(String text) {
        this(text, null, LEADING);
    }

    public GinjBorderedLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    public GinjBorderedLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    public GinjBorderedLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        setName("GinjBorderedLabel"); // To be addressed in laf.xml
    }
}
