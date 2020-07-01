package info.ginj;

import info.ginj.ui.GinjBorderedLabel;
import info.ginj.ui.GinjLabel;
import info.ginj.ui.Util;
import info.ginj.ui.WrapLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class HistoryFrame extends JFrame {
    public static final String THUMB_EXTENSION = "thumb.png";
    public static final String VIDEO_EXTENSION = "mp4";
    public static final String IMAGE_EXTENSION = "png";

    public static final Dimension HISTORY_CELL_SIZE = new Dimension(164, 164);
    public static final Dimension THUMBNAIL_SIZE = new Dimension(113, 91);
    public static final Dimension WINDOW_DEFAULT_SIZE = new Dimension(680, 550);

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

        JComponent historyPanel;
        final File[] files = Ginj.getHistoryFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));

        if (files == null) {
            Util.alertError(this, "History error", "Could not list files in history folder '" + Ginj.getHistoryFolder().getAbsolutePath() +"'");
            historyPanel = new JLabel("Error");
        }
        else {
            Arrays.sort(files); // Alphabetically by default

            JPanel historyList = new JPanel(new WrapLayout());
            for (File file : files) {
                historyList.add(new HistoryItemPanel(file));
            }

            historyPanel = new JScrollPane(historyList);
        }
        historyPanel.setPreferredSize(WINDOW_DEFAULT_SIZE);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(historyPanel, c);


        // Prepare status bar
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        final GinjBorderedLabel nameLabel = new GinjBorderedLabel("This is the history");
        statusPanel.add(nameLabel, BorderLayout.WEST);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(statusPanel, c);

        // Add default "draggable window" behaviour
        Util.addDraggableWindowMouseBehaviour(this, titleBar);

        // TODO should be resizeable with the bottom right corner handle (min 3x1)

        // Lay out components again
        pack();

        // Center window
        setLocationRelativeTo(null);
    }


    private void onClose() {
        // Close window
        dispose();
    }

    private void onDelete(int id) {
        // TODO ask the question: Also delete from storages (and list them) ?
    }


    //////////////////////////////
    // Inner classes

    public class HistoryEntry {
        Capture capture;
        String thumbnailImagePath;

        public Capture getCapture() {
            return capture;
        }

        public void setCapture(Capture capture) {
            this.capture = capture;
        }

        public String getThumbnailImagePath() {
            return thumbnailImagePath;
        }

        public void setThumbnailImagePath(String thumbnailImagePath) {
            this.thumbnailImagePath = thumbnailImagePath;
        }
    }


    private class HistoryItemPanel extends JPanel {
        private final String xmlFilename;
        private Capture capture = null;

        @Override
        public Dimension getPreferredSize() {
            return HISTORY_CELL_SIZE;
        }

        public HistoryItemPanel(File file) {
            super();
            xmlFilename = file.getAbsolutePath();

            setLayout(new GridBagLayout());

            setBackground(Color.DARK_GRAY);

            final JPanel imageLabel = new ThumbnailPanel(xmlFilename.substring(0, xmlFilename.lastIndexOf('.') + 1) + THUMB_EXTENSION);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            add(imageLabel, c);

            final JLabel nameLabel = new GinjLabel("?");
            try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)))) {
                capture = (Capture) xmlDecoder.readObject();
                nameLabel.setText(capture.getName()); // TODO truncate if too long
            }
            catch (Exception e) {
                Util.alertException(HistoryFrame.this, "Load error", "Error loading capture '" + file.getAbsolutePath() + "'", e);
                e.printStackTrace();
            }
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            add(nameLabel, c);

            final JLabel sizeLabel = new GinjLabel("?");
            File captureFile = new File(xmlFilename.substring(0, xmlFilename.lastIndexOf('.') + 1) + (capture.isVideo? VIDEO_EXTENSION : IMAGE_EXTENSION));
            sizeLabel.setText(Util.getPrettySize(captureFile.length()));
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            add(sizeLabel, c);
        }
    }

    private class ThumbnailPanel extends JPanel {

        private BufferedImage image = null;

        public ThumbnailPanel(String imagePath) {
            try {
                image = ImageIO.read(new File(imagePath));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Dimension getPreferredSize() {
            return THUMBNAIL_SIZE;
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image != null) {
                int x = (THUMBNAIL_SIZE.width - image.getWidth())/2;
                int y = (THUMBNAIL_SIZE.height - image.getHeight())/2;
                g.drawImage(image, x, y, image.getWidth(), image.getHeight(), this);
            }
            else {
                // Draw Text
                g.drawString("Error reading image", 0, 0);
            }
        }
    }
}
