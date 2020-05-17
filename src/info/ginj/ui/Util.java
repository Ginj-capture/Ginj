package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

public class Util {
    /**
     * Lay out components of a Panel and compute its size, like pack() for a Window.
     * This method computes the size of the given panel by adding it in a temporary window.
     * Warning, must be called before adding the panel to its final parent, because it will be removed from it otherwise
     * @return
     */
    public static Dimension packPanel(JPanel panel) {
        JWindow window = new JWindow();
        window.setLayout(new BorderLayout());
        window.getContentPane().add(panel);
        window.pack();
        final Dimension size = window.getSize();
        panel.setSize(size);
        return size;
    }
}
