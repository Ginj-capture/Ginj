package info.ginj.export.clipboard;

import info.ginj.Capture;
import info.ginj.export.GinjExporter;
import info.ginj.ui.Util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;

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
     * Copies the given capture to the clipboard
     *
     * @param capture        the capture to export
     * @param accountNumber  (ignored)
     * @return true if export completed, or false otherwise
     */
    @Override
    public boolean exportCapture(Capture capture, String accountNumber) {
        if (capture.isVideo()) {
            Util.alertError(getFrame(), "Export error", "Video contents cannot be copied to the clipboard");
            return false;
        }
        try {
            Image image = capture.getImage();
            if (image == null) {
                image = ImageIO.read(capture.getFile());
            }
            TransferableImage transferableImage = new TransferableImage(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(transferableImage, (clipboard1, contents) -> {
                // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
            });
            return true;
        }
        catch (Exception e) {
            Util.alertException(getFrame(), "Export error", "There was an error copying image to the clipboard", e);
            return false;
        }
    }
}
