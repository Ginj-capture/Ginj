package info.ginj;

import info.ginj.export.ExportMonitor;
import info.ginj.export.GinjExporter;
import info.ginj.ui.GinjLabel;
import info.ginj.ui.Util;

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

public class ExportFrame extends JFrame implements ExportMonitor {

    private final JLabel stateLabel;
    private final JLabel sizeLabel;
    private final BoundedRangeModel progressModel;
    private final Window parentWindow;
    private final Capture capture;
    private GinjExporter exporter;

    public ExportFrame(Window parentWindow, Capture capture, GinjExporter exporter) {
        super();
        this.parentWindow = parentWindow;
        this.capture = capture;
        this.exporter = exporter;

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
        Util.addDraggableWindowMouseBehaviour(this, mainPanel);

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
        sizeLabel.setText(Util.getPrettySizeRatio(currentSizeBytes, totalSizeBytes));
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
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     * @return true if export was started, false otherwise
     */
    public boolean startExport(String accountNumber) {
        if (exporter.prepare(capture, accountNumber)) {
            Thread exportThread = new Thread(() -> exporter.exportCapture(capture, accountNumber));
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
                Util.alertError(this, "Save error", "Could not create history folder (" + historyFolder.getAbsolutePath() + ")");
                return false;
            }
        }

        File targetFile = null;

        BufferedImage sourceImage = null;
        try {
            // Save capture itself
            if (capture.isVideo) {
                // Move file to history
                // TODO ENHANCEMENT move the source, not the rendered version !
                targetFile = new File(historyFolder, capture.getId() + ".mp4");
                Files.move(capture.transientGetFile().toPath(), targetFile.toPath());
            }
            else {
                // Write the image to disk
                targetFile = new File(historyFolder, capture.getId() + ".png");
                if (!ImageIO.write(capture.transientGetOriginalImage(), "png", targetFile)) {
                    Util.alertError(this, "Save error", "Writing capture to history failed (" + targetFile.getAbsolutePath() + ")");
                    return false;
                }
                sourceImage = capture.transientGetRenderedImage();
            }
        }
        catch (IOException e) {
            Util.alertException(this, "Save error", "Saving capture to history failed (" + targetFile.getAbsolutePath() + ")", e);
            return false;
        }


        // Save metadata and overlays to XML
        targetFile = new File(historyFolder, capture.getId() + ".xml");
        try (XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(targetFile)))) {
            xmlEncoder.writeObject(capture);
        }
        catch (Exception e) {
            Util.alertError(this, "Save error", "Saving metadata and overlays to history failed (" + targetFile.getAbsolutePath() + ")");
            return false;
        }


        // Save thumbnail
        if (sourceImage != null) {
            BufferedImage thumbnailImage;

            int sourceImageWidth = sourceImage.getWidth();
            int sourceImageHeight = sourceImage.getHeight();
            int thumbnailWidth = HistoryFrame.THUMBNAIL_SIZE.width;
            int thumbnailHeight = HistoryFrame.THUMBNAIL_SIZE.height;

            if (sourceImageWidth > thumbnailWidth || sourceImageHeight > thumbnailHeight) {
                // Resize
                double hScale = thumbnailWidth / ((double) sourceImageWidth);
                double vScale = thumbnailHeight / ((double) sourceImageHeight);
                double scale = Math.min(hScale, vScale);

                int targetWidth = (int) (sourceImageWidth * scale);
                int targetHeight = (int) (sourceImageHeight * scale);

                thumbnailImage = new BufferedImage(targetWidth, targetHeight, sourceImage.getType());
                AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
                AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);
                scaleOp.filter(sourceImage, thumbnailImage);
            }
            else {
                thumbnailImage = sourceImage;
            }

            // Write the thumbnail to disk
            try {
                targetFile = new File(historyFolder, capture.getId() + HistoryFrame.THUMB_EXTENSION);
                if (!ImageIO.write(thumbnailImage, "png", targetFile)) {
                    Util.alertError(this, "Save error", "Saving thumbnail to history failed (" + targetFile.getAbsolutePath() + ")");
                    return false;
                }
            }
            catch (IOException e) {
                Util.alertException(this, "Save error", "Saving thumbnail to history failed (" + targetFile.getAbsolutePath() + ")", e);
                return false;
            }
        }
        return true;
    }

}
