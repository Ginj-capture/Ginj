package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

// TODO for better-looking panels (rounded corners),
// see the "Working with Custom Painters" section of https://www.ibm.com/developerworks/library/j-synth/index.html
public class GinjBorderedPanel extends JPanel {
    public GinjBorderedPanel() {
        this(true);
    }

    public GinjBorderedPanel(LayoutManager layout) {
        this(layout, true);
    }

    public GinjBorderedPanel(boolean isDoubleBuffered) {
        this(new FlowLayout(), isDoubleBuffered);
    }

    public GinjBorderedPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        setName("GinjBorderedPanel"); // To be addressed in laf.xml
    }
}
