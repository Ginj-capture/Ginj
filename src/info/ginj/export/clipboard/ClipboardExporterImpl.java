package info.ginj.export.clipboard;

import info.ginj.export.ExportSettings;
import info.ginj.export.GinjExporter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;

/**
 * This exporter copies the image to the clipboard
 *
 * Based on code from https://coderanch.com/t/333565/java/BufferedImage-System-Clipboard
 */
public class ClipboardExporterImpl extends GinjExporter {

    public ClipboardExporterImpl(JFrame frame) {
        super(frame);
    }

    /**
     * Exports the given image
     * @param image the image to export
     * @param exportSettings a set of properties that could contain exporter-specific parameters
     * @return true if export completed, or false otherwise
     */
    @Override
    public boolean exportImage(BufferedImage image, ExportSettings exportSettings) {
        try {
            TransferableImage transferableImage = new TransferableImage(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(transferableImage, (clipboard1, contents) -> {
                // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
