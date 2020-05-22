package info.ginj.export;

import javax.swing.*;
import java.io.File;
import java.util.Properties;

public interface GinjExporter {
    void export(File file, Properties exportSessings, JFrame frame);
}
