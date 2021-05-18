package info.ginj.export;

import info.ginj.ui.StarWindow;

import javax.swing.*;
import java.awt.*;

public class ExportContext {
    private Component parentFrame;
    private StarWindow starWindow;
    private ExportMonitor exportMonitor;

    public ExportContext(JFrame parentFrame, StarWindow starWindow, ExportMonitor exportMonitor) {
        this.parentFrame = parentFrame;
        this.starWindow = starWindow;
        this.exportMonitor = exportMonitor;
    }

    public Component getParentFrame() {
        return parentFrame;
    }

    public void setParentFrame(Component parentFrame) {
        this.parentFrame = parentFrame;
    }

    public StarWindow getStarWindow() {
        return starWindow;
    }

    public void setStarWindow(StarWindow starWindow) {
        this.starWindow = starWindow;
    }

    public ExportMonitor getExportMonitor() {
        return exportMonitor;
    }

    public void setExportMonitor(ExportMonitor exportMonitor) {
        this.exportMonitor = exportMonitor;
    }
}
