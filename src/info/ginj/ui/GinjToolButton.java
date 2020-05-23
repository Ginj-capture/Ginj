package info.ginj.ui;

import javax.swing.*;

public class GinjToolButton extends JButton {
    public GinjToolButton() {
        this(null, null);
    }

    public GinjToolButton(Icon icon) {
        this(null, icon);
    }

    public GinjToolButton(String text, Icon icon) {
        super(text, icon);
        setName("GinjToolButton"); // To be addressed in laf.xml
    }
}
