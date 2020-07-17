package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.model.Prefs;
import info.ginj.util.UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This window centralizes optins
 */
public class MoreFrame extends JFrame {

    private final StarWindow starWindow;

    public MoreFrame(StarWindow starWindow) {
        super();
        this.starWindow = starWindow;

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " Options");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Prepare title bar
        JPanel titleBar = UI.getTitleBar(Ginj.getAppName() + " Options", e -> onClose());
        contentPane.add(titleBar, BorderLayout.NORTH);


        // Prepare main panel
        JComponent mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(0,1,20,20));
        mainPanel.setBorder(new EmptyBorder(new Insets(50, 50, 50, 50)));

        mainPanel.add(new JLabel("More options to come..."));
        // e.g. Capture folder
//        mainPanel.add(new JButton("Check for updates...")); // TODO

        final JButton configureTargetsButton = new JButton("Manage targets...");
        configureTargetsButton.addActionListener(e -> onConfigureTargets());
        mainPanel.add(configureTargetsButton);

        final JButton aboutButton = new JButton("About Ginj...");
        aboutButton.addActionListener(e -> onAbout());
        mainPanel.add(aboutButton);

        final JButton quitButton = new JButton("Quit " + Ginj.getAppName());
        quitButton.addActionListener(e -> onQuit());
        mainPanel.add(quitButton);

        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        // Lay out components again
        pack();

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    private void onConfigureTargets() {
        if (starWindow.getTargetManagementFrame() == null) {
            starWindow.setTargetManagementFrame(new TargetManagementFrame(starWindow));
        }
        starWindow.getTargetManagementFrame().setVisible(true);
        starWindow.getTargetManagementFrame().requestFocus();
    }

    private void onAbout() {
        JOptionPane.showMessageDialog(this, "This is " + Ginj.getAppName() + " version " + Ginj.getVersion() + "\nPlease checkout http://ginj.info for more information.", "Ajout Ginj", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onQuit() {
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure you want to exit " + Ginj.getAppName() + "?", "Quit Jing?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
            Prefs.save();
            System.exit(Ginj.ERR_STATUS_OK);
        }
    }

    private void onClose() {
        starWindow.setMoreFrame(null);
        // Close window
        dispose();
    }

}
