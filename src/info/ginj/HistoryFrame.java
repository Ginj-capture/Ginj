package info.ginj;

import info.ginj.ui.GinjLabel;
import info.ginj.ui.Util;

import javax.swing.*;
import java.awt.*;

public class HistoryFrame extends JFrame {

    public HistoryFrame() {
        super();

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " History");
        // this.setIconImage(); TODO

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // Prepare title bar
        JPanel titleBar = new JPanel();
        titleBar.setLayout(new BorderLayout());
        titleBar.setBackground(Color.YELLOW);
        JLabel testLabel = new JLabel("History");
        titleBar.add(testLabel, BorderLayout.CENTER);
        JButton closeButton = new JButton("X");
        closeButton.addActionListener(e -> onClose());
        titleBar.add(closeButton, BorderLayout.EAST);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(titleBar, c);

        // Prepare filter bar
        JPanel filterBar = new JPanel();
        filterBar.setOpaque(true);
        filterBar.setLayout(new GridLayout(1,5));
        filterBar.add(new JButton("Date"));
        filterBar.add(new JButton("Size"));
        filterBar.add(new JButton("Image"));
        filterBar.add(new JButton("Video"));
        filterBar.add(new JButton("Both"));

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        contentPane.add(filterBar, c);


        // Prepare an opaque panel which will fill the main display area and host the history scrollpane
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(true);
        mainPanel.setLayout(new GridLayout(2,1));

        final JRadioButton jRadioButton = new JRadioButton("Test A");
        jRadioButton.addActionListener(e -> {
            System.out.println(e);
        });
        mainPanel.add(jRadioButton);

        mainPanel.add(new JRadioButton("Test B"));
        mainPanel.setPreferredSize(new Dimension(600, 400));

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(mainPanel, c);


        // Prepare status bar
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        final GinjLabel nameLabel = new GinjLabel("This is the history");
        statusPanel.add(nameLabel, BorderLayout.WEST);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(statusPanel, c);

        // Add default "draggable window" behaviour
        Util.addDraggableWindowMouseBehaviour(this, titleBar);

        // Lay out components again
        pack();

        // Center window
        setLocationRelativeTo(null);
    }


    private void onClose() {
        // Close window
        dispose();
    }
}
