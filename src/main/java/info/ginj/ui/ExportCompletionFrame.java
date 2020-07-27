package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.export.clipboard.ClipboardExporter;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.Prefs;
import info.ginj.ui.component.YellowLabel;
import info.ginj.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This window appears when an export is complete, and optionally auto-closes
 */
public class ExportCompletionFrame extends JFrame {

    public static final int AUTO_HIDE_DELAY_SECS = 3;

    java.util.Timer autoHideTimer = null;
    int timeRemainingBeforeHide = AUTO_HIDE_DELAY_SECS;

    private JCheckBox autoHideCheckbox;

    public ExportCompletionFrame(Capture capture) {
        super();

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " export complete");
        setIconImage(StarWindow.getAppIcon());

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        // Add main label
        final List<Export> exports = capture.getExports();
        final Export export = exports.get(exports.size() - 1); // last export
        JLabel stateLabel = new YellowLabel(export.getExporterName() + " export complete");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(16, 16, 8, 16);
        mainPanel.add(stateLabel, c);

        // Add message label
        String html;
        if (ClipboardExporter.NAME.equals(export.getExporterName())) {
            html = "Your capture is ready to be pasted.";
        }
        else {
            if (export.getLocation() != null && export.getLocation().length() > 0) {
                html = "Your capture is available at the following location:<br/>"
                        + "<a href=\"" + export.getLocation() + "\">" + export.getLocation() + "</a>";
                if (export.isLocationCopied()) {
                    html += "<br/>That location is ready to be pasted.";
                }
            }
            else {
                html = "Your capture is now available.";
            }
        }
        // Let the "auto-hide" countdown active if the user clicks on the link.
        ActionListener linkClickListener = e -> {
            if (autoHideCheckbox.isSelected()) {
                startAutoHideTimer();
            }
        };

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(8, 16, 8, 16);
        mainPanel.add(UI.createClickableHtmlEditorPane(html, linkClickListener), c);

        // Add joke label
        JLabel jokelabel = new JLabel("Ginj will not be replaced - it's is here to stay :-).");

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 16, 8, 16);
        mainPanel.add(jokelabel, c);

        autoHideCheckbox = new JCheckBox();
        final boolean autoHide = Prefs.isTrue(Prefs.Key.EXPORT_COMPLETE_AUTOHIDE_KEY);
        autoHideCheckbox.setSelected(autoHide);
        if (autoHide) {
            startAutoHideTimer();
        }
        autoHideCheckbox.addActionListener(e -> onAutoHideChange());
        updateCheckboxText();
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 16, 16, 16);
        mainPanel.add(autoHideCheckbox, c);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> onClose());
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(8, 16, 16, 16);
        mainPanel.add(closeButton, c);

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, mainPanel);

        getContentPane().add(mainPanel);

        addMouseBehaviour(mainPanel);

        pack();

        UI.addEscKeyShortcut(this, e -> onClose());

        // Position window
        Ginj.starWindow.positionFrameNextToStarIcon(this);
    }

    private void onClose() {
        stopAutoHideTimer();
        dispose();
    }

    private void onAutoHideChange() {
        Prefs.set(Prefs.Key.EXPORT_COMPLETE_AUTOHIDE_KEY, String.valueOf(autoHideCheckbox.isSelected()));
        updateCheckboxText();
    }

    private void updateCheckboxText() {
        if (autoHideCheckbox.isSelected()) {
            autoHideCheckbox.setText("Auto-hide in " + timeRemainingBeforeHide + "...");
        }
        else {
            autoHideCheckbox.setText("Auto-hide          ");
        }
    }

    private void addMouseBehaviour(JComponent component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                stopAutoHideTimer();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Check if it is a real exit, not just hovering over button or checkbox
                if(!component.contains(e.getPoint())) {
                    // Real exit
                    if (autoHideCheckbox.isSelected()) {
                        startAutoHideTimer();
                    }
                }
            }

        });
    }

    private void startAutoHideTimer() {
        autoHideTimer = new Timer(true);
        autoHideTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeRemainingBeforeHide--;
                updateCheckboxText();
                if (timeRemainingBeforeHide <= 0) {
                    onClose();
                }
            }
        }, 950, 1000);
    }

    private void stopAutoHideTimer() {
        if (autoHideTimer != null) {
            autoHideTimer.cancel();
            autoHideTimer = null;
        }
    }

}
