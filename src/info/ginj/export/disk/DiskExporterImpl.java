package info.ginj.export.disk;

import info.ginj.export.GinjExporter;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DiskExporterImpl extends GinjExporter {

    public DiskExporterImpl(JFrame frame) {
        super(frame);
    }

    @Override
    public void export(Image image, Properties exportSettings) {
        JOptionPane.showMessageDialog(null, "Not implemented yet", "Save", JOptionPane.INFORMATION_MESSAGE);
    }
}
