package info.ginj.export;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Properties;

public abstract class GinjExporter {
    private JFrame frame;

    public GinjExporter(JFrame frame) {
        this.frame = frame;
    }

    public JFrame getFrame() {
        return frame;
    }

    public abstract void export(BufferedImage image, Properties exportSettings);
}
