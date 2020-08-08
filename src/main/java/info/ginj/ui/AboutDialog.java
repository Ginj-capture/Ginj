package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.util.UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;


/**
 * This window displays info about the app
 */
public class AboutDialog extends JDialog {

    private static StarWindow starWindow = null;

    public AboutDialog(StarWindow starWindow) {
        super();
        AboutDialog.starWindow = starWindow;

        // When entering a modal dialog, hotkey must be disabled, otherwise the app gets locked
        starWindow.unregisterHotKey();
        setModal(true);

        // For Alt+Tab behaviour
        this.setTitle("About " + Ginj.getAppName());
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final JPanel contentPane = new DoubleBorderedPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        // Prepare title bar
        JPanel titleBar = UI.getTitleBar("About " + Ginj.getAppName(), e -> onClose());
        contentPane.add(titleBar, BorderLayout.NORTH);


        // Prepare logo
        contentPane.add(new JLabel(new ImageIcon(getClass().getResource("/img/app-logo-180-opaque.png"))), BorderLayout.WEST);

        JEditorPane editorPane = UI.createClickableHtmlEditorPane("<b>" + Ginj.getAppName() + "</b> version " + Ginj.getVersion() + "<br/>" +
                "For all information please visit <a href=\"http://ginj.info\">ginj.info</a><br/>" +
                "Configuration:<br/>" +
                UI.readDisplayDetails());
        editorPane.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));
        contentPane.add(editorPane, BorderLayout.CENTER);

        // Prepare lower panel
        JPanel lowerPanel = new JPanel();
        lowerPanel.setOpaque(false);
        lowerPanel.setLayout(new FlowLayout());

        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> onClose());
        lowerPanel.add(closeButton);

        contentPane.add(lowerPanel, BorderLayout.SOUTH);

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        //setPreferredSize(WINDOW_PREFERRED_SIZE);
        pack();

        UI.addEscKeyShortcut(this, e -> onClose());

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    private void onClose() {
        // Restore the previous hotkey
        starWindow.registerHotKey();
        // Close window
        dispose();
    }
}
