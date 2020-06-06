package info.ginj.export.disk;

import info.ginj.export.GinjExporter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DiskExporterImpl extends GinjExporter {

    public DiskExporterImpl(JFrame frame) {
        super(frame);
    }

    @Override
    public void export(BufferedImage image, Properties exportSettings) {
        // TODO only prompt if required to do so in preferences
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save capture as...");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setSelectedFile(new File(exportSettings.getProperty("captureId") + ".png"));
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getAbsolutePath().toLowerCase().endsWith(".png");
            }

            @Override
            public String getDescription() {
                return "PNG images";
            }
        });
        if (fileChooser.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                ImageIO.write(image, "png", file);
            }
            catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(getFrame(), "Encoutered an error while saving image as\n" + file.getAbsolutePath() + "\n" + e.getMessage() + "\nMore info is available on the Java console", "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
