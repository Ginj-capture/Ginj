package info.ginj;

import info.ginj.ui.GinjButton;
import info.ginj.ui.GinjButtonBar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureEditingFrame extends JFrame {
    private BufferedImage capturedImg;
    private String captureId;

    public CaptureEditingFrame(BufferedImage capturedImg) {
        this(capturedImg, new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date())); // ENHANCEMENT
    }

    public CaptureEditingFrame(BufferedImage capturedImg, String captureId) {
        super();
        this.capturedImg = capturedImg;
        this.captureId = captureId;
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
        // Always store in history, no matter the export type
        saveInHistory();

        // TODO call the right Exporter implementation based on the exportType

        // and close Window
        dispose();
    }

    private void saveInHistory() {
        File historyFolder = new File("ZZhistoryFolder"); // TODO get from params
        if (!historyFolder.exists()) {
            historyFolder.mkdirs();
        }
        // Save image
        File file = new File(historyFolder, captureId + ".png");
        try {
            if (!ImageIO.write(capturedImg, "png", file)) {
                JOptionPane.showMessageDialog(null, "Capture failed (" + file.getAbsolutePath() + ")", "Screen capture error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage() + " - Full error on the console", "Screen capture error", JOptionPane.ERROR_MESSAGE);
        }

        // TODO save overlays to XML

    }

    private void onCancel() {
        // Close window
        dispose();
    }

    private void onCustomize() {
        // TODO
    }

}
