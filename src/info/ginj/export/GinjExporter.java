package info.ginj.export;

import info.ginj.Ginj;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class GinjExporter {
    private JFrame frame;

    public GinjExporter(JFrame frame) {
        this.frame = frame;
    }

    public JFrame getFrame() {
        return frame;
    }

    protected String getBaseFileName(ExportSettings exportSettings) {
        String baseFileName = exportSettings.getProperty(ExportSettings.KEY_CAPTURE_NAME);
        if (baseFileName == null || baseFileName.isBlank()) {
            baseFileName = exportSettings.getProperty(ExportSettings.KEY_CAPTURE_ID);
        }
        return baseFileName;
    }

    protected File imageToTempFile(BufferedImage image, ExportSettings exportSettings) throws IOException {
        File file = new File(Ginj.getTempDir(), exportSettings.getProperty(ExportSettings.KEY_CAPTURE_ID) + ".png");
        ImageIO.write(image, "png", file);
        file.deleteOnExit();
        return file;
    }

    /**
     * Copy the given String to the clipboard
     * @param text
     */
    protected void copyTextToClipboard(String text) {
        // Copy path to clipboard
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, (clipboard1, contents) -> {
            // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
        });
    }

    /**
     * Exports the given image
     *
     * @param image          the image to export
     * @param exportSettings a set of properties that could contain exporter-specific parameters
     * @return true if export completed, or false otherwise
     */
    public abstract boolean exportImage(BufferedImage image, ExportSettings exportSettings);


}
