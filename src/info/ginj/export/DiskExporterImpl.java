package info.ginj.export;

import javax.swing.*;
import java.io.File;
import java.util.Properties;

public class DiskExporterImpl implements GinjExporter {
    @Override
    public void export(File file, Properties exportSessings, JFrame frame) {

        // TODO additionally save in the requested folder, or prompt

        JOptionPane.showMessageDialog(null, "File:- " + file.getAbsolutePath(), "Screen Captured", JOptionPane.INFORMATION_MESSAGE);
    }
}
