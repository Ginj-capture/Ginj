package info.ginj.export.disk;

import info.ginj.Ginj;
import info.ginj.export.Exporter;
import info.ginj.model.Capture;
import info.ginj.model.Target;
import info.ginj.model.TargetPrefs;
import info.ginj.util.Misc;
import info.ginj.util.UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

/**
 * This exporter saves the image as a PNG file to disk, and optionally copies its path to the clipboard
 */
public class DiskExporter extends Exporter {

    public static final String NAME = "Disk";
    public static final int PROGRESS_SAVE_CALC_DESTINATION = 5;
    public static final int PROGRESS_SAVE = 50;
    private File destinationFile;


    @Override
    public String getExporterName() {
        return NAME;
    }

    @Override
    public String getDefaultShareText() {
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
     * @param target the target to export this capture to
     * @return true if we should continue, false to cancel export
     */
    @Override
    public boolean prepare(Capture capture, Target target) {
        logProgress("Determining target file", PROGRESS_SAVE_CALC_DESTINATION);
        // Determine where to save the file
        boolean askForLocation = Misc.isTrue(target.getOptions().get(TargetPrefs.ALWAYS_ASK_DIR_KEY));
        String saveDirName;
        if (askForLocation) {
            // If a default location is set and valid, use it
            saveDirName = target.getOptions().get(TargetPrefs.DEST_LOCATION_KEY);
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                // Otherwise default to "last saved", if defined and valid
                saveDirName = target.getOptions().get(TargetPrefs.LAST_CUSTOM_DEST_LOCATION_KEY);
                if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                    // Otherwise default to the current dir
                    saveDirName = new File("").getAbsolutePath();
                }
            }
        }
        else {
            // If a save location is set and valid, use it
            saveDirName = target.getOptions().get(TargetPrefs.DEST_LOCATION_KEY);
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                // Otherwise default to the current dir, and prompt
                saveDirName = new File("").getAbsolutePath();
                askForLocation = false;
            }
        }
        // Default file
        destinationFile = new File(saveDirName, capture.getDefaultName() + Misc.IMAGE_EXTENSION);

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
            JOptionPane.showMessageDialog(parentFrame, "Error opening file chooser: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        fileChooser.setDialogTitle("Save capture as...");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG (*" + Misc.IMAGE_EXTENSION + ")", Misc.IMAGE_EXTENSION.substring(1)));
        fileChooser.setSelectedFile(destinationFile);

        if (fileChooser.showSaveDialog(parentFrame) != JFileChooser.APPROVE_OPTION) {
            // Cancelled, closed or error
            return false;
        }

        destinationFile = fileChooser.getSelectedFile();
        if (!destinationFile.exists()) {
            // Selected file does not exist, go ahead
            return true;
        }

        // File exists, return true if user accepts overwrite
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(parentFrame, "Are you sure you want to overwrite: " + destinationFile.getAbsolutePath() + "\n?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Saves the given capture to disk
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture       the capture to export
     * @param target the target to export this capture to
     */
    @Override
    public void exportCapture(Capture capture, Target target) {
        try {
            logProgress("Saving capture", PROGRESS_SAVE);
            if (capture.isVideo()) {
                throw new RuntimeException("TODO video");
            }
            else {
                if (capture.getRenderedImage() != null) {
                    ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_PNG, destinationFile);
                }
                else {
                    // TODO make this a block copy loop that it can be cancelled
                    Files.copy(capture.getRenderedFile().toPath(), destinationFile.toPath());
                }
            }
        }
        catch (IOException e) {
            UI.alertException(parentFrame, "Save Error", "Encountered an error while saving image as\n'" + destinationFile.getAbsolutePath() + "'\n" + e.getMessage() + "\nMore info is available on the Java console", e);
            failed("Save error");
            return;
        }

        if (Misc.isTrue(target.getOptions().get(TargetPrefs.ALWAYS_ASK_DIR_KEY))) {
            // Remember selected path
            target.getOptions().put(TargetPrefs.LAST_CUSTOM_DEST_LOCATION_KEY, destinationFile.getParent());
            Ginj.getTargetPrefs().save();
        }

        String message = "Export completed successfully.";
        if (Misc.isTrue(target.getOptions().get(TargetPrefs.MUST_COPY_PATH_KEY))) {
            copyTextToClipboard(destinationFile.getAbsolutePath());
            message += "\nPath was copied to clipboard";
        }

        capture.addExport(getExporterName(), destinationFile.getAbsolutePath(), null);
        complete(message);
    }

}
