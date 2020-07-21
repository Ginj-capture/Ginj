package info.ginj.ui.component;

import javax.swing.*;
import java.awt.*;

/**
 * Used for sort and filter toggle buttons in history
 */
public class HistoryButtonPanel extends JPanel {
    public HistoryButtonPanel() {
        this(true);
    }

    public HistoryButtonPanel(LayoutManager layout) {
        this(layout, true);
    }

    public HistoryButtonPanel(boolean isDoubleBuffered) {
        this(new FlowLayout(), isDoubleBuffered);
    }

    public HistoryButtonPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        setName("HistoryButtonPanel"); // To be addressed in synth.xml
    }
}
