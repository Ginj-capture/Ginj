package info.ginj.export.disk;

import info.ginj.Ginj;
import info.ginj.export.ExportContext;
import info.ginj.export.ExportMonitor;
import info.ginj.export.Exporter;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.ExportSettings;
import info.ginj.model.Target;
import info.ginj.ui.StarWindow;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
     * @param target        the target to export this capture to
     * @return true if we should continue, false to cancel export
     */
    @Override
    public ExportContext prepare(JFrame parentFrame, StarWindow starWindow, ExportMonitor exportMonitor, Capture capture, Target target) {
        DiskExportContext context = new DiskExportContext(parentFrame, starWindow, exportMonitor);

        logProgress(context.getExportMonitor(), "Determining target file", PROGRESS_SAVE_CALC_DESTINATION);
        // Determine where to save the file
        boolean askForLocation =
                target.getSettings().getMustAlwaysAskLocation()
                || target.getSettings().getPreferredFileFormat() == ExportSettings.FileFormat.SELECT_ON_SAVE;

        String saveDirName = target.getSettings().getDestLocation();
        // Sanity check : if save location is not set or invalid, default to the previous "save location" setting, and force prompt
        if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
            saveDirName = target.getSettings().getLastCustomDestLocation();
            // If previous "save location" setting was not set or is invalid, default to the user dir
            if (saveDirName == null || saveDirName.isBlank() || !new File(saveDirName).exists()) {
                saveDirName = new File(System.getProperty("user.home")).getAbsolutePath();
            }
            askForLocation = true;
        }

        // Default destination file
        File destinationFile = new File(saveDirName, capture.computeUploadFilename());

        if (!askForLocation) {
            // Set destination file with default
            context.setDestinationFile(destinationFile);
            // OK, we're done.
            return context;
        }

        // Otherwise, we have to ask for optional changes to file location and name

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
        addDefaultNameAndFilters(fileChooser, capture, target, destinationFile);

        if (fileChooser.showSaveDialog(context.getParentFrame()) != JFileChooser.APPROVE_OPTION) {
            // Cancelled, closed or error
            return null;
        }

        destinationFile = fileChooser.getSelectedFile();

        // Make sure selected filename ends with the expected extension
        if (capture.isVideo()) {
            if (!destinationFile.getName().toLowerCase().endsWith(Misc.VIDEO_EXTENSION)) {
                destinationFile = new File(destinationFile.getAbsolutePath() + Misc.VIDEO_EXTENSION);
            }
        }
        else {
            switch (target.getSettings().getPreferredFileFormat()) {
                case JPEG:
                    if (!destinationFile.getName().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_JPEG)) {
                        destinationFile = new File(destinationFile.getAbsolutePath() + Misc.IMAGE_EXTENSION_JPEG);
                    }
                    break;
                case PNG:
                    if (!destinationFile.getName().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_PNG)) {
                        destinationFile = new File(destinationFile.getAbsolutePath() + Misc.IMAGE_EXTENSION_PNG);
                    }
                    break;
                case SELECT_ON_SAVE:
                    if (!destinationFile.getName().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_PNG) && !destinationFile.getName().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_JPEG)) {
                        // Let's force PNG
                        destinationFile = new File(destinationFile.getAbsolutePath() + Misc.IMAGE_EXTENSION_PNG);
                    }
                case SMALLEST:
                    if (!destinationFile.getName().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_PNG) && !destinationFile.getName().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_JPEG)) {
                        // Let's pretend PNG for now, could be changed later if needed
                        destinationFile = new File(destinationFile.getAbsolutePath() + Misc.IMAGE_EXTENSION_PNG);
                    }
            }
        }

        if (!destinationFile.exists()) {
            // Selected file does not exist, go ahead
            context.setDestinationFile(destinationFile);
            return context;
        }
        else {
            // File exists, return null if user refuses overwrite
            boolean overwrite = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(context.getParentFrame(), "Are you sure you want to overwrite: " + destinationFile.getAbsolutePath() + "\n?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (overwrite) {
                context.setDestinationFile(destinationFile);
                return context;
            }
            else {
                return null;
            }
        }
    }

    private void addDefaultNameAndFilters(JFileChooser fileChooser, Capture capture, Target target, File defaultDestinationFile) {

        // Clear previous filters, if any
        for (FileFilter choosableFileFilter : fileChooser.getChoosableFileFilters()) {
            fileChooser.removeChoosableFileFilter(choosableFileFilter);
        }

        FileNameExtensionFilter videoExtensionFilter = getFilenameExtensionFilter(Misc.VIDEO_EXTENSION);
        FileNameExtensionFilter pngExtensionFilter = getFilenameExtensionFilter(Misc.IMAGE_EXTENSION_PNG);
        FileNameExtensionFilter jpegExtensionFilter = getFilenameExtensionFilter(Misc.IMAGE_EXTENSION_JPEG);
        if (capture.isVideo()) {
            fileChooser.setFileFilter(videoExtensionFilter);
            fileChooser.setSelectedFile(defaultDestinationFile);
        }
        else {
            switch (target.getSettings().getPreferredFileFormat()) {
                case PNG:
                    fileChooser.setFileFilter(pngExtensionFilter);
                    fileChooser.setSelectedFile(defaultDestinationFile);
                    break;
                case JPEG:
                    fileChooser.setFileFilter(jpegExtensionFilter);
                    fileChooser.setSelectedFile(replaceExtension(defaultDestinationFile, Misc.IMAGE_EXTENSION_JPEG));
                    break;
                case SELECT_ON_SAVE:
                    fileChooser.addChoosableFileFilter(pngExtensionFilter);
                    fileChooser.addChoosableFileFilter(jpegExtensionFilter);
                    fileChooser.setSelectedFile(replaceExtension(defaultDestinationFile, Misc.IMAGE_EXTENSION_PNG));
                    break;
                case SMALLEST:
                    // No filter
                    fileChooser.setSelectedFile(replaceExtension(defaultDestinationFile, ""));
                    break;
            }
        }

        // Show the dialog *first* to ensure the UI is built
        SwingUtilities.invokeLater(() -> {
            // This runs AFTER the chooser is shown
            JTextField fileNameField = getFileNameTextField(fileChooser);

            // Listen to file filter changes to update the extension of the "selected" file
            fileChooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, evt -> {
                if (fileNameField != null) {
                    String filename = fileNameField.getText();
                    if (!filename.isEmpty()) {
                        FileFilter newFilter = fileChooser.getFileFilter();
                        String newExt = "";

                        if (newFilter == videoExtensionFilter) {
                            newExt = Misc.VIDEO_EXTENSION;
                        }
                        else if (newFilter == pngExtensionFilter) {
                            newExt = Misc.IMAGE_EXTENSION_PNG;
                        }
                        else if (newFilter == jpegExtensionFilter) {
                            newExt = Misc.IMAGE_EXTENSION_JPEG;
                        }

                        // Change file extension if needed
                        if (!filename.toLowerCase().endsWith(newExt)) {
                            String newName = filename.replaceAll("\\.[^.]+$", "") + newExt;
                            fileNameField.setText(newName);
                        }
                    }
                }
            });
        });
    }

    /**
     * Hacky way to access the filename text field before selection is confirmed
     * @param parent
     * @return
     */
    private static JTextField getFileNameTextField(Container parent) {
        for (Component comp : parent.getComponents()) {
            if (comp instanceof JTextField && comp.isVisible()) {
                return (JTextField) comp;
            }
            if (comp instanceof Container) {
                JTextField field = getFileNameTextField((Container) comp);
                if (field != null) return field;
            }
        }
        return null;
    }

    private static FileNameExtensionFilter getFilenameExtensionFilter(String extension1) {
        return new FileNameExtensionFilter(extension1.substring(1).toUpperCase() + " (*" + extension1 + ")", extension1.substring(1));
    }

    /**
     * Saves the given capture to disk
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture the capture to export
     * @param target  the target to export this capture to
     */
    @Override
    public void exportCapture(ExportContext context, Capture capture, Target target) {

        File destinationFile = ((DiskExportContext)context).getDestinationFile();
        try {
            logProgress(context.getExportMonitor(), "Saving capture", PROGRESS_SAVE);
            if (capture.isVideo() || capture.getRenderedImage() == null) {
                // TODO make this a block copy loop that can be cancelled (and doesn't freeze the UI) for large video files
                Files.copy(capture.getRenderedFile().toPath(), destinationFile.toPath());
            }
            else {
                // Save image
                switch (target.getSettings().getPreferredFileFormat()) {
                    case PNG -> ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_PNG, destinationFile);
                    case JPEG -> ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_JPEG, destinationFile);
                    case SELECT_ON_SAVE -> {
                        if (destinationFile.getAbsolutePath().toLowerCase().endsWith(Misc.IMAGE_EXTENSION_PNG)) {
                            ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_PNG, destinationFile);
                        }
                        else {
                            ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_JPEG, destinationFile);
                        }
                    }
                    case SMALLEST -> {
                        File tmpPng = Files.createTempFile(Paths.get(destinationFile.getParent()), null, null).toFile();
                        ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_PNG, tmpPng);
                        File tmpJpg = Files.createTempFile(Paths.get(destinationFile.getParent()), null, null).toFile();
                        ImageIO.write(capture.getRenderedImage(), Misc.IMAGE_FORMAT_JPEG, tmpJpg);
                        logger.info("PNG=" +  Misc.getPrettySize(tmpPng.length()) + " / JPG=" +  Misc.getPrettySize(tmpJpg.length()));
                        if (tmpJpg.length() < tmpPng.length()) {
                            destinationFile = replaceExtension(destinationFile, Misc.IMAGE_EXTENSION_JPEG);
                            keepFile(destinationFile, tmpJpg, tmpPng);
                        }
                        else {
                            destinationFile = replaceExtension(destinationFile, Misc.IMAGE_EXTENSION_PNG);
                            keepFile(destinationFile, tmpPng, tmpJpg);
                        }
                    }
                }

                // And remember path, if requested
                String destinationDir = destinationFile.getParent();
                if (target.getSettings().getMustRememberLastLocation()
                        && !destinationDir.equalsIgnoreCase(target.getSettings().getDestLocation())) {
                    target.getSettings().setDestLocation(destinationDir);
                    Ginj.getTargetPrefs().save();
                }
            }
        }
        catch (IOException e) {
            UI.alertException(context.getParentFrame(), "Save Error", "Encountered an error while saving image as\n'" + destinationFile.getAbsolutePath() + "'\n" + e.getMessage() + "\nMore info is available on the Java console", e, logger);
            failed(context, "Save error");
            return;
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

    private void keepFile(File targetFile, File fileToKeep, File fileToDelete) throws IOException {
        if (!fileToKeep.renameTo(targetFile)) {
            throw new IOException("Could not rename temp file '" + fileToKeep + "' to '" + targetFile + "'...");
        }
        if (!fileToDelete.delete()) {
            throw new IOException("Could not delete temp file '" + fileToDelete + "'...");
        }
    }

    private File replaceExtension(File file, String extension) {
        String fileName = file.getAbsolutePath();
        if (extension.isEmpty() || !fileName.endsWith(extension)) {
            return new File(fileName.substring(0, fileName.lastIndexOf('.')) + extension);
        }
        else {
            return file;
        }
    }

}
