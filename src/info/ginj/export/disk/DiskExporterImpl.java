package info.ginj.export.disk;

import info.ginj.Ginj;
import info.ginj.Prefs;
import info.ginj.export.ExportSettings;
import info.ginj.export.GinjExporter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * This exporter saves the image as a PNG file to disk, and copies its path to the clipboard
 */
public class DiskExporterImpl extends GinjExporter {

    public DiskExporterImpl(JFrame frame) {
        super(frame);
    }

    /**
     * Exports the given image to disk
     *
     * @param image          the image to export
     * @param exportSettings a set of properties that could contain exporter- and session- specific parameters
     * @return true if export completed, or false otherwise
     */
    @Override
    public boolean exportImage(BufferedImage image, ExportSettings exportSettings) {

        // Determine where to save the file
        boolean askForLocation = Prefs.isTrue(Prefs.Key.USE_CUSTOM_LOCATION);
        String saveDirName;
        if (askForLocation) {
            // If a default location is set and valid, use it
            saveDirName = Prefs.get(Prefs.Key.DEFAULT_CUSTOM_SAVE_LOCATION_DIR);
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                // Otherwise default to "last saved", if defined and valid
                saveDirName = Prefs.get(Prefs.Key.LAST_CUSTOM_SAVE_LOCATION_DIR);
                if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                    // Otherwise default to the current dir
                    saveDirName = new File("").getAbsolutePath();
                }
            }
        }
        else {
            // If a save location is set and valid, use it
            saveDirName = Prefs.get(Prefs.Key.SAVE_LOCATION_DIR);
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                // Otherwise default to the current dir, and prompt
                saveDirName = new File("").getAbsolutePath();
                askForLocation = false;
            }
        }
        // Default file
        File file = new File(saveDirName, getBaseFileName(exportSettings) + ".png");

        if (askForLocation) {
            JFileChooser fileChooser = null;
            // TODO does this bring real performance boost commpared to fileChooser = new JFileChooser(); ?
            try {
                fileChooser = Ginj.futureFileChooser.get();
            }
            catch (InterruptedException | ExecutionException e) {
                JOptionPane.showMessageDialog(getFrame(), "Error opening file chooser: " + e.getMessage());
                e.printStackTrace();
            }
            fileChooser.setDialogTitle("Save capture as...");
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
            fileChooser.setSelectedFile(file);

            if (fileChooser.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                if (file.exists()) {
                    if (JOptionPane.showConfirmDialog(getFrame(), "Are you sure you want to overwrite: " + file.getAbsolutePath() + "\n?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                        return false;
                    }
                }
            }
        }

        try {
            ImageIO.write(image, "png", file);
        }
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(getFrame(), "Encoutered an error while saving image as\n" + file.getAbsolutePath() + "\n" + e.getMessage() + "\nMore info is available on the Java console", "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (askForLocation) {
            // Remember selected path
            Prefs.set(Prefs.Key.LAST_CUSTOM_SAVE_LOCATION_DIR, file.getParent());
            Prefs.save();
        }

        copyTextToClipboard(file.getAbsolutePath());

        return true;
    }

}
