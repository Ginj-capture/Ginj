package info.ginj.export.disk;

import info.ginj.export.GinjExporter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * This exporter saves the image as a PNG file to disk, and copies its path to the clipboard
 */
public class DiskExporterImpl extends GinjExporter implements ClipboardOwner {

    public DiskExporterImpl(JFrame frame) {
        super(frame);
    }

    /**
     * Exports the given image
     *
     * @param image          the image to export
     * @param exportSettings a set of properties that could contain exporter-specific parameters
     * @return true if export completed, or false otherwise
     */
    @Override
    public boolean export(BufferedImage image, Properties exportSettings) {
        // TODO only prompt if required to do so in preferences, otherwise save in the default location
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save capture as...");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        fileChooser.setSelectedFile(new File(exportSettings.getProperty("captureId") + ".png"));
        if (fileChooser.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            boolean confirm = true;
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(getFrame(), "Are you sure you want to overwrite\n" + file.getAbsolutePath() + "\n?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                    confirm = false;
                }
            }
            if (confirm) {
                try {
                    ImageIO.write(image, "png", file);
                    StringSelection stringSelection = new StringSelection(file.getAbsolutePath());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, this);
                    return true;
                }
                catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getFrame(), "Encoutered an error while saving image as\n" + file.getAbsolutePath() + "\n" + e.getMessage() + "\nMore info is available on the Java console", "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return false;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
    }
}
