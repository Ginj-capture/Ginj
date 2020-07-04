package info.ginj.export.clipboard;

import info.ginj.export.GinjExporter;
import info.ginj.model.Capture;
import info.ginj.util.Util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;

/**
 * This exporter copies the image to the clipboard
 *
 * Based on code from https://coderanch.com/t/333565/java/BufferedImage-System-Clipboard
 */
public class ClipboardExporter extends GinjExporter {


    @Override
    public String getExporterName() {
        return "Clipboard";
    }

    @Override
    public String getShareText() {
        return "Copy";
    }

    @Override
    public String getIconPath() {
        return "/img/icon/copy.png";
    }

    @Override
    public boolean isOnlineService() {
        return false;
    }

    @Override
    public boolean isImageSupported() {
        return true;
    }

    @Override
    public boolean isVideoSupported() {
        return false;
    }

    /**
     * Copies the given capture to the clipboard
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture        the capture to export
     * @param accountNumber  (ignored)
     * @return true if export completed, or false otherwise
     */
    @Override
    public void exportCapture(Capture capture, String accountNumber) {
        if (capture.isVideo()) {
            Util.alertError(getParentFrame(), "Export error", "Video contents cannot be copied to the clipboard");
            failed("Error copying capture");
            return;
        }
        try {
            logProgress("Reading file", 50);
            Image image = capture.transientGetRenderedImage();
            if (image == null) {
                image = ImageIO.read(capture.transientGetFile());
            }
            TransferableImage transferableImage = new TransferableImage(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(transferableImage, (clipboard1, contents) -> {
                // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
            });
            capture.addExport(getExporterName(), null, null);
            complete("Image copied to clipboard");
        }
        catch (Exception e) {
            Util.alertException(getParentFrame(), "Export error", "There was an error copying image to the clipboard", e);
            failed("Error copying capture");
        }
    }
}
