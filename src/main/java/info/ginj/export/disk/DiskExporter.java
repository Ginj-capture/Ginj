package info.ginj.export.disk;

import info.ginj.Ginj;
import info.ginj.export.GinjExporter;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;
import info.ginj.util.Util;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

/**
 * This exporter saves the image as a PNG file to disk, and copies its path to the clipboard
 */
public class DiskExporter extends GinjExporter {

    private File targetFile;


    @Override
    public String getExporterName() {
        return "Disk";
    }

    @Override
    public String getShareText() {
        return "Save";
    }

    @Override
    public String getIconPath() {
        return "/img/icon/save.png";
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
        return true;
    }

    /**
     * Prepares the exporter for the export.
     * This method is run in Swing's Event Dispatching Thread before launching the actual export.
     * If needed, we prompt user for target file.
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     * @return true if we should continue, false to cancel export
     */
    @Override
    public boolean prepare(Capture capture, String accountNumber) {
        logProgress("Determining target file", 5);
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
        targetFile = new File(saveDirName, capture.getDefaultName() + Ginj.IMAGE_EXTENSION);

        if (!askForLocation) {
            // OK, we're done.
            return true;
        }

        // Ask for location
        JFileChooser fileChooser;
        try {
            // TODO does this bring real performance boost compared to fileChooser = new JFileChooser(); ?
            fileChooser = Ginj.futureFileChooser.get();
        }
        catch (InterruptedException | ExecutionException e) {
            JOptionPane.showMessageDialog(getParentFrame(), "Error opening file chooser: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        fileChooser.setDialogTitle("Save capture as...");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG (*" +Ginj.IMAGE_EXTENSION + ")", Ginj.IMAGE_EXTENSION.substring(1)));
        fileChooser.setSelectedFile(targetFile);

        if (fileChooser.showSaveDialog(getParentFrame()) != JFileChooser.APPROVE_OPTION) {
            // Cancelled, closed or error
            return false;
        }

        targetFile = fileChooser.getSelectedFile();
        if (!targetFile.exists()) {
            // Selected file does not exist, go ahead
            return true;
        }

        // File exists, return true if user accepts overwrite
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(getParentFrame(), "Are you sure you want to overwrite: " + targetFile.getAbsolutePath() + "\n?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Saves the given capture to disk
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture       the capture to export
     * @param accountNumber (ignored)
     */
    @Override
    public void exportCapture(Capture capture, String accountNumber) {
        try {
            logProgress("Saving image", 50);
            if (capture.transientGetRenderedImage() != null) {
                ImageIO.write(capture.transientGetRenderedImage(), Ginj.IMAGE_FORMAT_PNG, targetFile);
            }
            else {
                // TODO make this a block copy loop that can be cancelled
                Files.copy(capture.transientGetFile().toPath(), targetFile.toPath());
            }
        }
        catch (IOException e) {
            Util.alertException(getParentFrame(), "Save Error", "Encountered an error while saving image as\n'" + targetFile.getAbsolutePath() + "'\n" + e.getMessage() + "\nMore info is available on the Java console", e);
            failed("Save error");
            return;
        }

        if (Prefs.isTrue(Prefs.Key.USE_CUSTOM_LOCATION)) {
            // Remember selected path
            Prefs.set(Prefs.Key.LAST_CUSTOM_SAVE_LOCATION_DIR, targetFile.getParent());
            Prefs.save();
        }

        copyTextToClipboard(targetFile.getAbsolutePath());
        capture.addExport(getExporterName(), targetFile.getAbsolutePath(), null);
        complete("Path copied to clipboard");
    }

}
