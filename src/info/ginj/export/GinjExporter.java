package info.ginj.export;

import info.ginj.Capture;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public abstract class GinjExporter {
    private JFrame frame;

    public GinjExporter(JFrame frame) {
        this.frame = frame;
    }

    public JFrame getFrame() {
        return frame;
    }


    /**
     * Copy the given String to the clipboard
     * @param text
     */
    protected void copyTextToClipboard(String text) {
        // Copy path to clipboard
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, (clipboard1, contents) -> {
            // Do nothing. It's normal to lose ownership when another app copies something to the clipboard
        });
    }


    /**
     * Exports the given capture
     *
     * @param capture        the capture to export
     * @param accountNumber  the accountNumber to export this capture to (if relevant)
     * @return true if export completed, or false otherwise
     */
    public abstract boolean exportCapture(Capture capture, String accountNumber);
}
