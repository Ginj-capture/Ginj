package info.ginj;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CaptureEditingFrame extends JFrame {
    private BufferedImage capturedImg;

    public CaptureEditingFrame(BufferedImage capturedImg) {
        super();
        this.capturedImg = capturedImg;
        final Dimension capturedImgSize = new Dimension(capturedImg.getWidth(), capturedImg.getHeight());

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

        getContentPane().add(scrollableImagePanel);
        pack();
    }
}
