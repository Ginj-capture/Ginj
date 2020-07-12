package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.export.ExportMonitor;
import info.ginj.export.Exporter;
import info.ginj.model.Capture;
import info.ginj.model.Target;
import info.ginj.ui.component.GinjLabel;
import info.ginj.util.Misc;
import info.ginj.util.UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This "small" progress window is responsible for starting, monitoring, and controlling an export in background.
 */
public class ExportFrame extends JFrame implements ExportMonitor {

    private final JLabel stateLabel;
    private final JLabel sizeLabel;
    private final BoundedRangeModel progressModel;
    private final Window parentWindow;
    private final Capture capture;
    private Exporter exporter;

    public ExportFrame(Window parentWindow, Capture capture, Exporter exporter) {
        super();
        this.parentWindow = parentWindow;
        this.capture = capture;
        this.exporter = exporter;

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " Export");
        setIconImage(StarWindow.getAppIcon());

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        // Add state label
        stateLabel = new GinjLabel("Exporting...");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 16, 0, 16);
        mainPanel.add(stateLabel, c);


        // Add progress bar
        progressModel = new DefaultBoundedRangeModel();
        JProgressBar progressBar = new JProgressBar(progressModel);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1;
        c.insets = new Insets(4, 16, 4, 16);
        mainPanel.add(progressBar, c);

        if (exporter != null) {
            // Add cancel button
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> onCancel());

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 0, 16);
            mainPanel.add(cancelButton, c);
        }


        // Add size label
        sizeLabel = new GinjLabel(" ");

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 16, 4, 16);
        mainPanel.add(sizeLabel, c);


        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, mainPanel);

        getContentPane().add(mainPanel);

        pack();
        setSize(280,70);

        // Center window
        // TODO should pop up next to the star icon
        setLocationRelativeTo(null);
    }


    @Override
    public void log(String state, int progress, long currentSizeBytes, long totalSizeBytes) {
        stateLabel.setText(state);
        progressModel.setValue(progress);
        sizeLabel.setText(Misc.getPrettySizeRatio(currentSizeBytes, totalSizeBytes));
    }

    @Override
    public void log(String state, int progress, String sizeProgress) {
        stateLabel.setText(state);
        progressModel.setValue(progress);
        sizeLabel.setText(sizeProgress);
    }

    @Override
    public void log(String state, int progress) {
        stateLabel.setText(state);
        progressModel.setValue(progress);
    }

    @Override
    public void log(String state) {
        stateLabel.setText(state);
    }

    private void onCancel() {
        if (exporter != null) {
            exporter.cancel();
        }
        // "Reopen" the capture window
        if (parentWindow != null) {
            parentWindow.setVisible(true);
        }

        closeExportWindow();
    }

    @Override
    public void complete(String state) {
        // Free up capture Window
        if (parentWindow != null) {
            parentWindow.dispose();
        }

        // Store image in history, no matter the export type
        saveToHistory(capture);

        closeExportWindow();

        // TODO Should be the notification window with auto close
        JOptionPane.showMessageDialog(this, state, "Export complete", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void failed(String state) {
        // "Reopen" the capture window
        if (parentWindow != null) {
            parentWindow.setVisible(true);
        }

        closeExportWindow();
    }

    private void closeExportWindow() {
        // Close this window
        exporter = null;
        dispose();
    }


    /**
     * Prepares and starts the export
     * @param target the target to export this capture to
     * @return true if export was started, false otherwise
     */
    public boolean startExport(Target target) {
        if (exporter.prepare(capture, target)) {
            Thread exportThread = new Thread(() -> exporter.exportCapture(capture, target));
            exportThread.start();
            return true;
        }
        else {
            closeExportWindow();
            return false;
        }
    }


    private boolean saveToHistory(Capture capture) {
        File historyFolder = Ginj.getHistoryFolder();
        if (!historyFolder.exists()) {
            if (!historyFolder.mkdirs()) {
                UI.alertError(this, "Save error", "Could not create history folder (" + historyFolder.getAbsolutePath() + ")");
                return false;
            }
        }

        // Save the original file to history
        // ENHANCEMENT we store the source, not the rendered version !
        // Compute filename (no version involved here)
        File originalFile = new File(historyFolder, capture.getId() + (capture.isVideo()? Misc.VIDEO_EXTENSION: Misc.IMAGE_EXTENSION));
        try {
            // Original file could be shared between multiple captures, only store it once
            if (!originalFile.exists()) {
                // Save capture itself
                if (capture.getOriginalFile() != null) {
                    // Move file to history
                    Files.move(capture.getOriginalFile().toPath(), originalFile.toPath());
                }
                else {
                    // No original file on disk, write image from memory (should not be null !)
                    if (!ImageIO.write(capture.getOriginalImage(), Misc.IMAGE_FORMAT_PNG, originalFile)) {
                        UI.alertError(this, "Save error", "Writing capture to history failed (" + originalFile.getAbsolutePath() + ")");
                        return false;
                    }
                }
            }
        }
        catch (IOException e) {
            UI.alertException(this, "Save error", "Saving capture to history failed (" + originalFile.getAbsolutePath() + ")", e);
            return false;
        }

        // Save metadata and overlays to XML
        // Compute filename (including version)
        File metadataFile = new File(historyFolder, capture.getBaseFilename() + Misc.METADATA_EXTENSION);
        try (XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(metadataFile)))) {
            xmlEncoder.writeObject(capture);
        }
        catch (Exception e) {
            UI.alertError(this, "Save error", "Saving metadata and overlays to history failed (" + metadataFile.getAbsolutePath() + ")");
            return false;
        }


        // Save thumbnail
        if (capture.isVideo()) {
            throw new RuntimeException("TODO video thumbnail");
        }
        else {
            BufferedImage thumbnailSourceImage = capture.getRenderedImage();
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
                    UI.alertError(this, "Save error", "Saving thumbnail to history failed (" + thumbnailFile.getAbsolutePath() + ")");
                    return false;
                }
            }
            catch (IOException e) {
                UI.alertException(this, "Save error", "Saving thumbnail to history failed (" + thumbnailFile.getAbsolutePath() + ")", e);
                return false;
            }
        }

        if (Ginj.starWindow.getHistoryFrame() != null) {
            Ginj.starWindow.getHistoryFrame().refreshHistoryList();
        }
        return true;
    }

}
