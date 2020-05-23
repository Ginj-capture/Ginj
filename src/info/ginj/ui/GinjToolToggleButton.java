package info.ginj.ui;

import javax.swing.*;

public class GinjToolToggleButton extends JToggleButton {
    public GinjToolToggleButton() {
        this(null, null, false);
    }

    public GinjToolToggleButton(Icon icon) {
        this(null, icon, false);
    }

    public GinjToolToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public GinjToolToggleButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setName("GinjToolButton"); // To be addressed in laf.xml
    }
}
