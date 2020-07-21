package info.ginj.ui.component;

import javax.swing.*;
import java.awt.*;

public class YellowBorderedPanel extends JPanel {
    public YellowBorderedPanel() {
        this(true);
    }

    public YellowBorderedPanel(LayoutManager layout) {
        this(layout, true);
    }

    public YellowBorderedPanel(boolean isDoubleBuffered) {
        this(new FlowLayout(), isDoubleBuffered);
    }

    public YellowBorderedPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        setName("YellowBorderedPanel"); // To be addressed in synth.xml
    }
}
