package info.ginj.export.disk;

import info.ginj.Ginj;
import info.ginj.export.ExportContext;
import info.ginj.export.ExportMonitor;
import info.ginj.export.Exporter;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.Target;
import info.ginj.ui.StarWindow;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(DiskExporter.class);

    public static final String NAME = "Disk";
    public static final int PROGRESS_SAVE_CALC_DESTINATION = 5;
    public static final int PROGRESS_SAVE = 50;


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
     * @param parentFrame
     * @param starWindow
     * @param exportMonitor
     * @param capture       the capture to export
     * @param target the target to export this capture to
     * @return true if we should continue, false to cancel export
     */
    @Override
    public ExportContext prepare(JFrame parentFrame, StarWindow starWindow, ExportMonitor exportMonitor, Capture capture, Target target) {
        DiskExportContext context = new DiskExportContext(parentFrame, starWindow, exportMonitor);

        logProgress(context.getExportMonitor(), "Determining target file", PROGRESS_SAVE_CALC_DESTINATION);
        // Determine where to save the file
        boolean askForLocation = target.getSettings().getMustAlwaysAskLocation();
        String saveDirName;
        if (askForLocation) {
            // If a default location is set and valid, use it
            saveDirName = target.getSettings().getDestLocation();
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                // Otherwise default to "last saved", if defined and valid
                saveDirName = target.getSettings().getLastCustomDestLocation();
                if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                    // Otherwise default to the current dir
                    saveDirName = new File("").getAbsolutePath();
                }
            }
        }
        else {
            // If a save location is set and valid, use it
            saveDirName = target.getSettings().getDestLocation();
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                // Otherwise default to the current dir, and prompt
                saveDirName = new File("").getAbsolutePath();
                askForLocation = false;
            }
        }
        // Default file
        File destinationFile = new File(saveDirName, capture.computeUploadFilename());
        context.setDestinationFile(destinationFile);

        if (!askForLocation) {
            // OK, we're done.
            return context;
        }

        // Ask for location
        String extension = capture.computeExtension();
        JFileChooser fileChooser;
        try {
            // TODO does this bring real performance boost compared to fileChooser = new JFileChooser(); ?
            fileChooser = Ginj.futureFileChooser.get();
        }
        catch (InterruptedException | ExecutionException e) {
            UI.alertException(context.getParentFrame(), "Save error", "Error opening file chooser", e, logger);
            return null;
        }
        fileChooser.setDialogTitle("Save capture as...");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter(extension.substring(1).toUpperCase() + " (*" + extension + ")", extension.substring(1)));
        fileChooser.setSelectedFile(destinationFile);

        if (fileChooser.showSaveDialog(context.getParentFrame()) != JFileChooser.APPROVE_OPTION) {
            // Cancelled, closed or error
            return null;
        }

        destinationFile = fileChooser.getSelectedFile();
        // Make sure it ends with the default extension
        if (!destinationFile.getName().toLowerCase().endsWith(extension)) {
            destinationFile = new File(destinationFile.getAbsolutePath() + extension);
        }
        if (!destinationFile.exists()) {
            // Selected file does not exist, go ahead
            return context;
        }

        // File exists, return null if user refuses overwrite
        boolean overwrite = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(context.getParentFrame(), "Are you sure you want to overwrite: " + destinationFile.getAbsolutePath() + "\n?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (overwrite) {
            return context;
        }
        else {
            return null;
        }
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
    public void exportCapture(ExportContext context, Capture capture, Target target) {

        File destinationFile = ((DiskExportContext)context).getDestinationFile();
        try {
            logProgress(context.getExportMonitor(), "Saving capture", PROGRESS_SAVE);
            if (capture.isVideo() || capture.getRenderedImage() == null) {
                // TODO make this a block copy loop that it can be cancelled (and doesn't freeze the UI) for large files
                Files.copy(capture.getRenderedFile().toPath(), destinationFile.toPath());
            }
            else {
                ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_PNG, destinationFile);
//                if (remember && ask) {
//                    target.getSettings().setLastCustomDestLocation(destinationFile.getParent());
//                }
            }
        }
        catch (IOException e) {
            UI.alertException(context.getParentFrame(), "Save Error", "Encountered an error while saving image as\n'" + destinationFile.getAbsolutePath() + "'\n" + e.getMessage() + "\nMore info is available on the Java console", e, logger);
            failed(context, "Save error");
            return;
        }

        if (target.getSettings().getMustAlwaysAskLocation()) {
            // Remember selected path
            target.getSettings().setLastCustomDestLocation(destinationFile.getParent());
            Ginj.getTargetPrefs().save();
        }

        Export export = new Export(getExporterName(), destinationFile.getAbsolutePath(), destinationFile.getAbsolutePath(), false);

        String message = "Export completed successfully.";
        if (target.getSettings().getMustCopyPath()) {
            copyTextToClipboard(export.getLocation());
            export.setLocationCopied(true);
            message += "\nPath was copied to clipboard";
        }

        capture.addExport(export);
        complete(context, capture, message);
    }

}
