package info.ginj.export.clipboard;

import info.ginj.export.GinjExporter;
import info.ginj.model.Capture;
import info.ginj.model.Target;
import info.ginj.util.UI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;

/**
 * This exporter copies the image to the clipboard
 *
 * Based on code from https://coderanch.com/t/333565/java/BufferedImage-System-Clipboard
 */
public class ClipboardExporter extends GinjExporter {


    public static final String NAME = "Clipboard";

    @Override
    public String getExporterName() {
        return NAME;
    }

    @Override
    public String getDefaultShareText() {
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
     * @param target the target to export this capture to
     * @return true if export completed, or false otherwise
     */
    @Override
    public void exportCapture(Capture capture, Target target) {
        if (capture.isVideo()) {
            UI.alertError(parentFrame, "Export error", "Video contents cannot be copied to the clipboard");
            failed("Error copying capture");
            return;
        }
        try {
            logProgress("Reading source", 50);
            Image image = capture.getRenderedImage();
            if (image == null) {
                image = ImageIO.read(capture.getOriginalFile());
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
            UI.alertException(parentFrame, "Export error", "There was an error copying image to the clipboard", e);
            failed("Error copying capture");
        }
    }
}
