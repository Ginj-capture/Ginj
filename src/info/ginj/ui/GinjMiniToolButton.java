package info.ginj.ui;

import javax.swing.*;

public class GinjMiniToolButton extends JButton {
    public GinjMiniToolButton() {
        this(null, null);
    }

    public GinjMiniToolButton(Icon icon) {
        this(null, icon);
    }

    public GinjMiniToolButton(String text, Icon icon) {
        super(text, icon);
        setName("GinjMiniToolButton"); // To be addressed in laf.xml
    }
}
