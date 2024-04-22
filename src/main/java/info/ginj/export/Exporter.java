package info.ginj.export;

import info.ginj.Ginj;
import info.ginj.export.clipboard.ClipboardExporter;
import info.ginj.export.disk.DiskExporter;
import info.ginj.export.online.dropbox.DropboxExporter;
import info.ginj.export.online.google.GoogleDriveExporter;
import info.ginj.export.online.google.GooglePhotosExporter;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.Prefs;
import info.ginj.model.Target;
import info.ginj.ui.ExportCompletionFrame;
import info.ginj.ui.HistoryFrame;
import info.ginj.ui.StarWindow;
import info.ginj.util.Jaffree;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class Exporter implements Cancellable {
    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    // Caching
    private static ArrayList<Exporter> exporters;

    /**
     * This static method returns a list of all exporters.
     *
     * @return a list containing an instance of all available exporters
     */
    @java.beans.Transient
    public static List<Exporter> getList() {
        if (exporters == null) {
            exporters = new ArrayList<>();
            exporters.add(new DiskExporter());
            exporters.add(new ClipboardExporter());
            exporters.add(new DropboxExporter());
            exporters.add(new GooglePhotosExporter());
            exporters.add(new GoogleDriveExporter());
        }
        return exporters;
    }

    @java.beans.Transient
    public abstract String getExporterName();

    @java.beans.Transient
    public abstract String getDefaultShareText();

    @java.beans.Transient
    public abstract String getIconPath();

    @java.beans.Transient
    public abstract boolean isOnlineService();

    @java.beans.Transient
    public abstract boolean isImageSupported();

    @java.beans.Transient
    public abstract boolean isVideoSupported();

    @java.beans.Transient
    public ImageIcon getButtonIcon(int size) {
        if (isOnlineService()) {
            // Use official logo and don't colorize
            return UI.createIcon(getClass().getResource(getIconPath()), size, size);
        }
        else {
            // Colorize
            return UI.createIcon(getClass().getResource(getIconPath()), size, size, UI.ICON_ENABLED_COLOR);
        }
    }


    /**
     * Prepares the exporter for the export.
     * This method is run in Swing's Event Dispatching Thread before launching the actual export.
     * It's the right time to e.g. prompt user for additional information before launching the export.
     *
     *
     * @param parentFrame
     * @param starWindow
     * @param exportMonitor
     * @param capture the capture to export
     * @param target  the target to export this capture to
     * @return an ExportContext object to pass the capture phase if we should continue, or null to cancel export
     */
    public abstract ExportContext prepare(JFrame parentFrame, StarWindow starWindow, ExportMonitor exportMonitor, Capture capture, Target target);

    /**
     * Exports the given capture.
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture the capture to export
     * @param target  the target to export this capture to
     */
    public abstract void exportCapture(ExportContext exportContext, Capture capture, Target target);


    /////////////////////////
    // Progress logging
    //


    protected void logProgress(ExportMonitor monitor, String state, int value, long currentSizeBytes, long totalSizeBytes) {
        if (monitor != null) monitor.log(state, value, currentSizeBytes, totalSizeBytes);
    }

    protected void logProgress(ExportMonitor exportMonitor, String state, int value) {
        if (exportMonitor != null) exportMonitor.log(state, value);
    }

    protected void logProgress(ExportMonitor exportMonitor, String state) {
        if (exportMonitor != null) exportMonitor.log(state);
    }

    protected void complete(ExportContext context, Capture capture, String state) {
        logger.debug("Exporter.complete");
        if (context.getExportMonitor() != null) context.getExportMonitor().complete(state);

        // Store image in history, no matter the export type
        saveToHistory(context, capture);

        final List<Export> exports = capture.getExports();
        final Export export = exports.get(exports.size() - 1); // last export

        if (Prefs.isTrue(Prefs.Key.USE_TRAY_NOTIFICATION_ON_EXPORT_COMPLETION) && context.getStarWindow().isTrayAvailable()) {
            context.getStarWindow().popupTrayNotification(export);
        }
        else {
            // Open the "capture completion" notification window with auto-hide
            new ExportCompletionFrame(export).setVisible(true);
        }
    }

    // TODO Should probably be in a separate thread
    private boolean saveToHistory(ExportContext context, Capture capture) {
        Component parentFrame = context.getParentFrame();
        File historyFolder = Ginj.getHistoryFolder();
        if (!historyFolder.exists()) {
            if (!historyFolder.mkdirs()) {
                UI.alertError(parentFrame, "Save error", "Could not create history folder (" + historyFolder.getAbsolutePath() + ")");
                return false;
            }
        }

        // Save the original file to history
        // ENHANCEMENT we store the source, not the rendered version !
        // Compute filename (no version involved here)
        File originalFile = new File(historyFolder, capture.getId() + capture.defaultExtension());
        try {
            // Original file could be shared between multiple captures, only store it once
            if (!originalFile.exists()) {
                // Save capture itself
                if (capture.getOriginalFile() != null) {
                    // Move file to history
                    Files.move(capture.getOriginalFile().toPath(), originalFile.toPath());
                }
                else {
                    if (capture.isVideo()) {
                        UI.alertError(parentFrame, "Save error", "Cannot move original video file to history: capture.getOriginalFile() is null!");
                        return false;
                    }
                    else {
                        // No original file on disk, write image from memory (should not be null !)
                        if (!ImageIO.write(capture.getOriginalImage(), Misc.IMAGE_FORMAT_PNG, originalFile)) {
                            UI.alertError(parentFrame, "Save error", "Writing capture to history failed (" + originalFile.getAbsolutePath() + ")");
                            return false;
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            UI.alertException(parentFrame, "Save error", "Saving capture to history failed (" + originalFile.getAbsolutePath() + ")", e, logger);
            return false;
        }

        // Save metadata and overlays to XML
        // Compute filename (including version)
        File metadataFile = new File(historyFolder, capture.getBaseFilename() + Misc.METADATA_EXTENSION);
        try (XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(metadataFile)))) {
            capture.tearDown();
            xmlEncoder.writeObject(capture);
        }
        catch (Exception e) {
            UI.alertError(parentFrame, "Save error", "Saving metadata and overlays to history failed (" + metadataFile.getAbsolutePath() + ")");
            return false;
        }


        // Save thumbnail
        BufferedImage thumbnailSourceImage;
        if (capture.isVideo()) {
            // TODO grab from rendered video, not from original
            thumbnailSourceImage = Jaffree.grabImage(originalFile, 0);
        }
        else {
            thumbnailSourceImage = capture.getRenderedImage();
        }

        BufferedImage thumbnailImage;

        int sourceImageWidth = thumbnailSourceImage.getWidth();
        int sourceImageHeight = thumbnailSourceImage.getHeight();
        int thumbnailWidth = HistoryFrame.THUMBNAIL_SIZE.width;
        int thumbnailHeight = HistoryFrame.THUMBNAIL_SIZE.height;

        if (sourceImageWidth > thumbnailWidth || sourceImageHeight > thumbnailHeight) {
            // Resize
            double hScale = thumbnailWidth / ((double) sourceImageWidth);
            double vScale = thumbnailHeight / ((double) sourceImageHeight);
            double scale = Math.min(hScale, vScale);

            int targetWidth = (int) (sourceImageWidth * scale);
            int targetHeight = (int) (sourceImageHeight * scale);

            thumbnailImage = new BufferedImage(targetWidth, targetHeight, thumbnailSourceImage.getType());
            AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
            AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);
            scaleOp.filter(thumbnailSourceImage, thumbnailImage);
        }
        else {
            thumbnailImage = thumbnailSourceImage;
        }

        // Write the thumbnail to disk
        // Compute filename (including version)
        File thumbnailFile = new File(historyFolder, capture.getBaseFilename() + Misc.THUMBNAIL_EXTENSION);
        try {
            if (!ImageIO.write(thumbnailImage, Misc.IMAGE_FORMAT_PNG, thumbnailFile)) {
                UI.alertError(parentFrame, "Save error", "Saving thumbnail to history failed (" + thumbnailFile.getAbsolutePath() + ")");
                return false;
            }
        }
        catch (IOException e) {
            UI.alertException(parentFrame, "Save error", "Saving thumbnail to history failed (" + thumbnailFile.getAbsolutePath() + ")", e, logger);
            return false;
        }


        if (Ginj.starWindow.getHistoryFrame() != null) {
            Ginj.starWindow.getHistoryFrame().loadHistoryList();
        }
        return true;
    }


    protected void failed(ExportContext context, String state) {
        logger.debug("Exporter.failed");
        if (context.getExportMonitor() != null) context.getExportMonitor().failed(state);
    }


    ////////////////////////
    // Utils

    /**
     * Copy the given String to the clipboard
     *
     * @param text to copy to the clipboard
     */
    protected void copyTextToClipboard(String text) {
        // Copy path to clipboard
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, (clipboard1, contents) -> {
            // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
        });
    }


    @Override
    public void cancel(ExportContext context) {
        logger.debug("Exporter.cancel");
        // "Reopen" the capture window
        if (context.getParentFrame() != null) {
            context.getParentFrame().setVisible(true);
        }
    }

    // TODO check this regularly at execution level
    protected boolean isCancelRequested() {
        return false /* ?? we should have one per export in progress */;
    }

    @Override
    public String toString() {
        return getExporterName();
    }
}
