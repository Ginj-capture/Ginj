package info.ginj.ui.component;

import javax.swing.*;
import java.awt.*;

// TODO for better-looking panels (rounded corners), see the "Working with Custom Painters" section of https://www.ibm.com/developerworks/library/j-synth/index.html
// See also http://www.jyloo.com/news/?pubId=1268844895000
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
