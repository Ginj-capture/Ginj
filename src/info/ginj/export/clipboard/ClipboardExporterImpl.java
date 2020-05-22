package info.ginj.export.clipboard;

import info.ginj.export.GinjExporter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.Properties;

/**
 * Based on code from https://coderanch.com/t/333565/java/BufferedImage-System-Clipboard
 */
public class ClipboardExporterImpl extends GinjExporter implements ClipboardOwner {

    public ClipboardExporterImpl(JFrame frame) {
        super(frame);
    }

    @Override
    public void export(Image image, Properties exportSettings) {
        TransferableImage transferableImage = new TransferableImage(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableImage, this);
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        JOptionPane.showMessageDialog(getFrame(), "Lost clipboard ownership !", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
