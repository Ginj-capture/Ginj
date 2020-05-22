package info.ginj;

import info.ginj.ui.GinjButton;
import info.ginj.ui.GinjButtonBar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class CaptureEditingFrame extends JFrame {
    private BufferedImage capturedImg;

    public CaptureEditingFrame(BufferedImage capturedImg) {
        super();
        this.capturedImg = capturedImg;
        final Dimension capturedImgSize = new Dimension(capturedImg.getWidth(), capturedImg.getHeight());

        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.drawImage(capturedImg, 0, 0, this);
            }

            @Override
            public Dimension getPreferredSize() {
                return capturedImgSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return capturedImgSize;
            }

            @Override
            public Dimension getMinimumSize() {
                return capturedImgSize;
            }

            @Override
            public Dimension getSize(Dimension rv) {
                return capturedImgSize;
            }
        };

        JScrollPane scrollableImagePanel = new JScrollPane(imagePanel);

        contentPane.add(scrollableImagePanel, BorderLayout.CENTER);

        // Prepare button bar
        JPanel actionPanel = new JPanel(); // To add a margin around buttonBar
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actionPanel.setName("GinjPanel"); // To be used as a selector in laf.xml
        JPanel buttonBar = new GinjButtonBar();
        try {
            GinjButton shareButton = new GinjButton("Share via X", new ImageIcon(ImageIO.read(getClass().getResource("img/b_share.png"))));
            shareButton.addActionListener(e -> onExport("share"));
            buttonBar.add(shareButton);
            GinjButton saveButton = new GinjButton("Save", new ImageIcon(ImageIO.read(getClass().getResource("img/b_save.png"))));
            saveButton.addActionListener(e -> onExport("disk"));
            buttonBar.add(saveButton);
            final JButton copyButton = new GinjButton("Copy", new ImageIcon(ImageIO.read(getClass().getResource("img/b_copy.png"))));
            copyButton.addActionListener(e -> onExport("clipboard"));
            buttonBar.add(copyButton);
            final JButton cancelButton = new GinjButton("Cancel", new ImageIcon(ImageIO.read(getClass().getResource("img/b_cancel.png"))));
            cancelButton.addActionListener(e -> onCancel());
            buttonBar.add(cancelButton);
            final JButton customizeButton = new GinjButton("Customize Ginj buttons", new ImageIcon(ImageIO.read(getClass().getResource("img/b_customize.png"))));
            customizeButton.addActionListener(e -> onCustomize());
            buttonBar.add(customizeButton);
        }
        catch (IOException e) {
            System.out.println("Error loading capture button images");
            e.printStackTrace();
            System.exit(Ginj.ERR_STATUS_LOAD_IMG);
        }
        actionPanel.add(buttonBar);

        contentPane.add(actionPanel, BorderLayout.SOUTH);

        pack();

        // Center window
        setLocationRelativeTo(null);
    }

    private void onExport(String exportType) {

    }

    private void onCancel() {
        dispose();
    }

    private void onCustomize() {

    }

}
