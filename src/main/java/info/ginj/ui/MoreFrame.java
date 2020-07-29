package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.util.Misc;
import info.ginj.util.UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This dialog centralizes access to settings, targets, and other features
 */
public class MoreFrame extends JFrame {

    private final StarWindow starWindow;

    public MoreFrame(StarWindow starWindow) {
        super();
        this.starWindow = starWindow;

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " more...");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final JPanel contentPane = new DoubleBorderedPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        // Prepare title bar
        JPanel titleBar = UI.getTitleBar("More...", e -> onClose());
        contentPane.add(titleBar, BorderLayout.NORTH);


        // Prepare main panel
        JComponent mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new GridLayout(0,1,20,20));
        mainPanel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

        final JButton optionsButton = new JButton("Options...");
        optionsButton.addActionListener(e -> onOptions());
        mainPanel.add(optionsButton);

        final JButton manageTargetsButton = new JButton("Manage targets...");
        manageTargetsButton.addActionListener(e -> onManageTargets());
        mainPanel.add(manageTargetsButton);

        final JButton checkForUpdatesButton = new JButton("Check for updates...");
        checkForUpdatesButton.addActionListener(e -> starWindow.onCheckForUpdates());
        mainPanel.add(checkForUpdatesButton);

        final JButton aboutButton = new JButton("About Ginj...");
        aboutButton.addActionListener(e -> onAbout());
        mainPanel.add(aboutButton);

        final JButton exitButton = new JButton(Misc.getExitQuitText() + " " + Ginj.getAppName());
        exitButton.addActionListener(e -> starWindow.onExit());
        mainPanel.add(exitButton);

        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        // Lay out components again
        pack();

        UI.addEscKeyShortcut(this, e -> onClose());

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    private void onManageTargets() {
        starWindow.openTargetManagementFrame();
    }

    private void onOptions() {
        new OptionsDialog(starWindow).setVisible(true);
    }

    private void onAbout() {
        new AboutDialog(starWindow).setVisible(true);
    }


    private void onClose() {
        starWindow.clearMoreFrame();
        // Restore the previous hotkey
        starWindow.registerHotKey();
        // Close window
        dispose();
    }

}
