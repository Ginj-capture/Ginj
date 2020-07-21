package info.ginj.ui.component;

import javax.swing.*;

/**
 * Toggle buttons used for history filter and sort
 */
public class HistoryToggleButton extends JToggleButton {
    public HistoryToggleButton() {
        this(null, null, false);
    }

    public HistoryToggleButton(Icon icon) {
        this(null, icon, false);
    }

    public HistoryToggleButton(String text) {
        this(text, null, false);
    }

    public HistoryToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public HistoryToggleButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setName("HistoryToggleButton"); // To be addressed in synth.xml
    }
}
