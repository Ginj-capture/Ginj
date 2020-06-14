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

    /**
     * Exports the given image
     * @param image the image to export
     * @param exportSettings a set of properties that could contain exporter-specific parameters
     * @return true if export completed, or false otherwise
     */
    public abstract boolean exportImage(BufferedImage image, Properties exportSettings);
}
