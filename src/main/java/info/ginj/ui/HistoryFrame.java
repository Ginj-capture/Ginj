package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.model.Capture;
import info.ginj.ui.component.GinjBorderedLabel;
import info.ginj.ui.component.GinjLabel;
import info.ginj.ui.layout.WrapLayout;
import info.ginj.util.Util;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * This window displays and manages the historized captures
 */
public class HistoryFrame extends JFrame {

    public static final Dimension HISTORY_CELL_SIZE = new Dimension(156, 164);
    public static final Dimension THUMBNAIL_SIZE = new Dimension(113, 91);
    public static final Dimension MAIN_AREA_DEFAULT_SIZE = new Dimension(680, 466);
    private final ImageIcon exportIcon;
    private final ImageIcon editIcon;
    private final ImageIcon deleteIcon;
    private final Color defaultBgColor;
//    private final Color defaultLabelForeground;

    private StarWindow parentWindow;
    private HistoryItemPanel selectedItem;
    private final JPanel historyList;

    public HistoryFrame(StarWindow parentWindow) {
        super();
        this.parentWindow = parentWindow;

        exportIcon = Util.createIcon(getClass().getResource("/img/icon/export.png"), 16, 16, Util.ICON_ENABLED_COLOR);
        editIcon = Util.createIcon(getClass().getResource("/img/icon/edit.png"), 16, 16, Util.ICON_ENABLED_COLOR);
        deleteIcon = Util.createIcon(getClass().getResource("/img/icon/delete.png"), 16, 16, Util.ICON_ENABLED_COLOR);
        defaultBgColor = getBackground();

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " History");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // Add title bar
        final JPanel titleBar = Util.getTitleBar("History", e -> onClose());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(titleBar, c);

        // Prepare filter bar
        JPanel filterBar = new JPanel();
        filterBar.setOpaque(true);
        filterBar.setLayout(new GridLayout(1,5));
        filterBar.add(new JButton("Name"));
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
        historyList = new JPanel(new WrapLayout(WrapLayout.LEFT));

        refreshHistoryList();

        historyPanel = new JScrollPane(historyList);
        historyPanel.setPreferredSize(MAIN_AREA_DEFAULT_SIZE);

        historyPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                setSelectedItem(null);
            }
        });

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
        final GinjBorderedLabel nameLabel = new GinjBorderedLabel(Ginj.getAppName() + " is brought to you by a random guy.");
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

    public void refreshHistoryList() {
        historyList.removeAll();
        final File[] files = Ginj.getHistoryFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(Ginj.METADATA_EXTENSION));

        if (files == null) {
            Util.alertError(this, "History error", "Could not list files in history folder '" + Ginj.getHistoryFolder().getAbsolutePath() +"'");
            historyList.add(new JLabel("Error"));
        }
        else {
            Arrays.sort(files); // Alphabetically by default

            for (File file : files) {
                historyList.add(new HistoryItemPanel(this, file));
            }
        }
        historyList.validate();
    }


    private void onClose() {
        parentWindow.setHistoryFrame(null);
        // Close window
        dispose();
    }

    private void onEdit(Capture capture) {
        // TODO should create a copy of the capture and open the edit window on it
        // Q: Do we duplicate the source media ? Would be silly
        //    If not, a delete should not delete the source media while it's still in use by at least one capture !
        //    Maybe Use a naming convention, like capture_id = <orig_capture_id> + "_" + <number>
    }

    private void onExport(Capture capture) {
        // TODO should copy the shared URL back to the clipboard, except for Clipboard that should re-execute an "export"
    }


    private void onDelete(Capture capture) {
        // TODO ask the question: Also delete from storages (and list them) ?
        // TODO if re-exported captures point to the same source, only delete the source media when it's the last one
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "The selected capture will be deleted from the History.\nFor now, the exported version (if any) will remain untouched.\nAre you sure you want to delete capture '" + capture.getName() + "'?", "Delete Capture", JOptionPane.YES_NO_OPTION)) {
            boolean ok = new File(Ginj.getHistoryFolder(), capture.getId() + Ginj.METADATA_EXTENSION).delete();
            ok = ok && new File(Ginj.getHistoryFolder(), capture.getId() + Ginj.THUMBNAIL_EXTENSION).delete();
            ok = ok && new File(Ginj.getHistoryFolder(), capture.getId() + (capture.isVideo() ? Ginj.VIDEO_EXTENSION : Ginj.IMAGE_EXTENSION)).delete();
            if (!ok) {
                Util.alertError(this, "Delete error", "There was an error deleting history files for catpure id '" + capture.getId() + "'!");
            }
            refreshHistoryList();
        }
    }

    public HistoryItemPanel getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(HistoryItemPanel selectedItem) {
        // Deselect previous one, if any
        if (this.selectedItem != null) {
            this.selectedItem.setSelected(false);
        }
        // Select new one, if any
        if (selectedItem != null) {
            selectedItem.setSelected(true);
        }
        // and remember it
        this.selectedItem = selectedItem;
    }


    //////////////////////////////
    // Inner classes

    private class HistoryItemPanel extends JPanel {
        private final String xmlFilename;
        private Capture capture = null;
        private final JLabel nameLabel;
        private final JLabel sizeLabel;
        private final JButton editButton;
        private final JButton exportButton;
        private final JButton deleteButton;

        @Override
        public Dimension getPreferredSize() {
            return HISTORY_CELL_SIZE;
        }

        public HistoryItemPanel(HistoryFrame historyFrame, File file) {
            super();
            xmlFilename = file.getAbsolutePath();

            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(5, 5, 5, 5));

            final JPanel imageLabel = new ThumbnailPanel(xmlFilename.substring(0, xmlFilename.lastIndexOf('.')) + Ginj.THUMBNAIL_EXTENSION);
            imageLabel.setBackground(null);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            c.insets = new Insets(10, 10, 10, 10);
            add(imageLabel, c);

            nameLabel = new GinjLabel("?");
            nameLabel.setBackground(null);
            nameLabel.setPreferredSize(new Dimension(90, 16));
            try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)))) {
                capture = (Capture) xmlDecoder.readObject();
                nameLabel.setText(capture.getName());
                nameLabel.setToolTipText(capture.getName());
                nameLabel.addMouseListener(new MouseAdapter() {
                    // Trick to keep clickability while showing tooltip, taken from https://stackoverflow.com/a/14932443/13551878
                    public void mouseReleased(MouseEvent e) {
                        HistoryItemPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, HistoryItemPanel.this));
                    }
                });
            }
            catch (Exception e) {
                Util.alertException(HistoryFrame.this, "Load error", "Error loading capture '" + file.getAbsolutePath() + "'", e);
                e.printStackTrace();
            }
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            add(nameLabel, c);

            sizeLabel = new GinjLabel("?");
            sizeLabel.setBackground(null);
            sizeLabel.setPreferredSize(new Dimension(55, 16));
            File captureFile = new File(xmlFilename.substring(0, xmlFilename.lastIndexOf('.')) + (capture.isVideo()? Ginj.VIDEO_EXTENSION : Ginj.IMAGE_EXTENSION));
            sizeLabel.setText(Util.getPrettySize(captureFile.length()));
            sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            add(sizeLabel, c);

            JPanel buttonBar = new JPanel(new GridLayout(1, 3, 5, 0));
            buttonBar.setBackground(null);
            buttonBar.setBorder(new EmptyBorder(2, 0, 0, 0));
            editButton = new JButton(editIcon);
            editButton.addActionListener(e -> onEdit(capture));

            exportButton = new JButton(exportIcon);
            exportButton.addActionListener(e -> onExport(capture));

            deleteButton = new JButton(deleteIcon);
            deleteButton.addActionListener(e -> onDelete(capture));
            // Hide buttons by default
            editButton.setVisible(false);
            exportButton.setVisible(false);
            deleteButton.setVisible(false);

            buttonBar.add(editButton);
            buttonBar.add(exportButton);
            buttonBar.add(deleteButton);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            add(buttonBar, c);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    historyFrame.setSelectedItem(HistoryItemPanel.this);
                }
            });
        }

        public void setSelected(boolean selected) {
            if (selected) {
                this.setBackground(Util.HISTORY_SELECTED_ITEM_BACKGROUND_COLOR);
                nameLabel.setForeground(Color.BLACK);
                sizeLabel.setForeground(Color.BLACK);
                editButton.setVisible(true);
                // exportButton.setVisible(true);
                deleteButton.setVisible(true);
            }
            else {
                this.setBackground(null);
                nameLabel.setForeground(Util.LABEL_FOREGROUND_COLOR);
                sizeLabel.setForeground(Util.LABEL_FOREGROUND_COLOR);
                editButton.setVisible(false);
                // exportButton.setVisible(false);
                deleteButton.setVisible(false);
            }
        }
    }

    private class ThumbnailPanel extends JPanel {

        private BufferedImage image = null;

        public ThumbnailPanel(String imagePath) {
            try {
                image = ImageIO.read(new File(imagePath));
            }
            catch (Exception e) {
                System.err.println("Error reading '" + imagePath + "'...");
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
