package info.ginj.export;

import info.ginj.model.Capture;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GinjExporter implements Cancellable {
    private final JFrame frame;
    private ExportMonitor exportMonitor;

    public abstract String getExporterName();

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public GinjExporter(JFrame frame) {
        this.frame = frame;
    }

    public JFrame getFrame() {
        return frame;
    }

    public ExportMonitor getExportMonitor() {
        return exportMonitor;
    }

    public void setExportMonitor(ExportMonitor logger) {
        exportMonitor = logger;
    }

    /**
     * Prepares the exporter for the export.
     * This method is run in Swing's Event Dispatching Thread before launching the actual export.
     * It's the right time to prompt user for additional information before launching the export.
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     * @return true if we should continue, false to cancel export
     */
    public boolean prepare(Capture capture, String accountNumber) {
        // Do nothing by default
        return true;
    }

    /**
     * Exports the given capture.
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     */
    public abstract void exportCapture(Capture capture, String accountNumber);


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

    protected boolean isCancelled() {
        return cancelRequested.get();
    }
}
