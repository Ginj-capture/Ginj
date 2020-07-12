package info.ginj.export;

import info.ginj.export.clipboard.ClipboardExporter;
import info.ginj.export.disk.DiskExporter;
import info.ginj.export.online.dropbox.DropboxExporter;
import info.ginj.export.online.google.GooglePhotosExporter;
import info.ginj.model.Capture;
import info.ginj.model.Target;
import info.ginj.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Exporter implements Cancellable {

    // TODO mabye make the following fields ThreadLocal ?
    protected JFrame parentFrame;
    private ExportMonitor exportMonitor;

    /**
     * This static method returns an list of all exporters.
     *
     * @return a list containing an instance of all available exporters
     */
    @java.beans.Transient
    public static List<Exporter> getList() {
        final ArrayList<Exporter> exporters = new ArrayList<>();
        exporters.add(new DiskExporter());
        exporters.add(new ClipboardExporter());
        exporters.add(new DropboxExporter());
        exporters.add(new GooglePhotosExporter());
        return exporters;
    }

    @java.beans.Transient
    public abstract String getExporterName();

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public void initialize(JFrame parentFrame, ExportMonitor logger) {
        this.parentFrame = parentFrame;
        exportMonitor = logger;
    }

    /**
     * Prepares the exporter for the export.
     * This method is run in Swing's Event Dispatching Thread before launching the actual export.
     * It's the right time to e.g.  prompt user for additional information before launching the export.
     *
     * @param capture the capture to export
     * @param target  the target to export this capture to
     * @return true if we should continue, false to cancel export
     */
    public boolean prepare(Capture capture, Target target) {
        // Do nothing by default
        return true;
    }

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
     * Exports the given capture.
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture the capture to export
     * @param target  the target to export this capture to
     */
    public abstract void exportCapture(Capture capture, Target target);


    /////////////////////////
    // Progress logging
    //


    protected void logProgress(String state, int value, long currentSizeBytes, long totalSizeBytes) {
        if (exportMonitor != null) exportMonitor.log(state, value, currentSizeBytes, totalSizeBytes);
    }

    protected void logProgress(String state, int value) {
        if (exportMonitor != null) exportMonitor.log(state, value);
    }

    protected void logProgress(String state) {
        if (exportMonitor != null) exportMonitor.log(state);
    }

    protected void complete(String state) {
        if (exportMonitor != null) exportMonitor.complete(state);
    }

    protected void failed(String state) {
        if (exportMonitor != null) exportMonitor.failed(state);
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


    // TODO check this regularly at execution level
    @Override
    public void cancel() {
        cancelRequested.set(true);
    }

    protected boolean cancelRequested() {
        return cancelRequested.get();
    }

    @Override
    public String toString() {
        return getExporterName();
    }
}
