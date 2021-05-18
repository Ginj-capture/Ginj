package info.ginj.export.disk;

import info.ginj.export.ExportContext;
import info.ginj.export.ExportMonitor;
import info.ginj.ui.StarWindow;

import javax.swing.*;
import java.io.File;

public class DiskExportContext extends ExportContext {
    private File destinationFile;

    public DiskExportContext(JFrame parentFrame, StarWindow starWindow, ExportMonitor exportMonitor) {
        super(parentFrame, starWindow, exportMonitor);
    }

    public File getDestinationFile() {
        return destinationFile;
    }

    public void setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
    }
}
