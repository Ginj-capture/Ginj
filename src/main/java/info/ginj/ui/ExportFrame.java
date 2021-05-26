package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.export.ExportMonitor;
import info.ginj.ui.component.YellowLabel;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * This "small" progress window is responsible for starting, monitoring, and controlling an export in background.
 */
public class ExportFrame extends JFrame implements ExportMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ExportFrame.class);
    public static final String MSG_CANCELLATION_REQUESTED = "Cancellation requested";

    private JLabel stateLabel;
    private JLabel sizeLabel;
    private BoundedRangeModel progressModel;
    private Window parentWindow;
    private boolean isCancelRequested = false;
    private final JButton cancelButton;

    public ExportFrame(Window parentWindow) {
        super();
        logger.debug("ExportFrame.constructor");
        this.parentWindow = parentWindow;

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " Export");
        setIconImage(StarWindow.getAppIcon());

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        // Add state label
        stateLabel = new YellowLabel("Exporting...");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 16, 0, 16);
        mainPanel.add(stateLabel, c);


        // Add progress bar
        progressModel = new DefaultBoundedRangeModel();
        JProgressBar progressBar = new JProgressBar(progressModel);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1;
        c.insets = new Insets(4, 16, 4, 16);
        mainPanel.add(progressBar, c);

        // Add cancel button
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 16);
        mainPanel.add(cancelButton, c);

        // Add size label
        sizeLabel = new YellowLabel(" ");

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 16, 4, 16);
        mainPanel.add(sizeLabel, c);


        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, mainPanel);

        getContentPane().add(mainPanel);

        pack();
        setSize(280, 70);

        UI.addEscKeyShortcut(this, e -> onCancel());

        // Position window
        Ginj.starWindow.positionFrameNextToStarIcon(this);

        logger.debug("ExportFrame: setVisible(true)");
        setVisible(true);
    }

    @Override
    public void log(String state, int progress, long currentSizeBytes, long totalSizeBytes) {
        stateLabel.setText(state);
        progressModel.setValue(progress);
        sizeLabel.setText(Misc.getPrettySizeRatio(currentSizeBytes, totalSizeBytes));
    }

    @Override
    public void log(String state, int progress, String sizeProgress) {
        stateLabel.setText(state);
        progressModel.setValue(progress);
        sizeLabel.setText(sizeProgress);
    }

    @Override
    public void log(String state, int progress) {
        stateLabel.setText(state);
        progressModel.setValue(progress);
    }

    @Override
    public void log(String state) {
        stateLabel.setText(state);
    }

    private void onCancel() {
        logger.debug("ExportFrame.onCancel");
        isCancelRequested = true;
        if (MSG_CANCELLATION_REQUESTED.equals(stateLabel.getText())) {
            logger.debug("(second call)");
            // Second time "cancel" is called. Something weird happened... Closing?
            // TODO prompt to send log
            close();
        }
        else {
            stateLabel.setText(MSG_CANCELLATION_REQUESTED);
            cancelButton.setText("Close");
        }
    }

    @Override
    public boolean isCancelRequested() {
        logger.debug("ExportFrame.isCancelRequested");
        if (isCancelRequested) {
            // This request is taken into account, close the monitor
            close();
        }
        return isCancelRequested;
    }

    @Override
    public void complete(String state) {
        logger.debug("ExportFrame.complete");
        close();
    }

    @Override
    public void failed(String state) {
        logger.debug("ExportFrame.failed");
        // "Reopen" the capture window
        if (parentWindow != null) {
            logger.debug("ExportFrame: Showing parent Window");
            parentWindow.setVisible(true);
        }
        close();
    }

    public void close() {
        logger.debug("ExportFrame.close");
        stateLabel = null;
        sizeLabel = null;
        progressModel = null;
        parentWindow = null;

        setVisible(false);

        dispose();
    }
}
