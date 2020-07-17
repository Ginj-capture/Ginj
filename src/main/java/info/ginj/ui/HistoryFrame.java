package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.model.Capture;
import info.ginj.ui.component.GinjBorderedLabel;
import info.ginj.ui.component.GinjLabel;
import info.ginj.ui.layout.WrapLayout;
import info.ginj.util.Misc;
import info.ginj.util.UI;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private final StarWindow starWindow;
    private HistoryItemPanel selectedItem;
    private final JPanel historyList;

    public HistoryFrame(StarWindow starWindow) {
        super();
        this.starWindow = starWindow;

        exportIcon = UI.createIcon(getClass().getResource("/img/icon/export.png"), 16, 16, UI.ICON_ENABLED_COLOR);
        editIcon = UI.createIcon(getClass().getResource("/img/icon/edit.png"), 16, 16, UI.ICON_ENABLED_COLOR);
        deleteIcon = UI.createIcon(getClass().getResource("/img/icon/delete.png"), 16, 16, UI.ICON_ENABLED_COLOR);

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
        final JPanel titleBar = UI.getTitleBar("History", e -> onClose());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(titleBar, c);

        // Prepare filter bar
        JPanel filterBar = new JPanel();
        filterBar.setOpaque(true);
        filterBar.setLayout(new GridLayout(1,5));
        final JButton sortByNameButton = new JButton("Name");
        sortByNameButton.addActionListener(e -> UI.featureNotImplementedDialog(HistoryFrame.this));
        filterBar.add(sortByNameButton);
        final JButton sortByDateButton = new JButton("Date");
        sortByDateButton.addActionListener(e -> UI.featureNotImplementedDialog(HistoryFrame.this));
        filterBar.add(sortByDateButton);
        final JButton sortBySizeButton = new JButton("Size");
        sortBySizeButton.addActionListener(e -> UI.featureNotImplementedDialog(HistoryFrame.this));
        filterBar.add(sortBySizeButton);
        final JButton showImageButton = new JButton("Image");
        showImageButton.addActionListener(e -> UI.featureNotImplementedDialog(HistoryFrame.this));
        filterBar.add(showImageButton);
        final JButton showVideoButton = new JButton("Video");
        showVideoButton.addActionListener(e -> UI.featureNotImplementedDialog(HistoryFrame.this));
        filterBar.add(showVideoButton);
        final JButton showBothButton = new JButton("Both");
        showBothButton.addActionListener(e -> UI.featureNotImplementedDialog(HistoryFrame.this));
        filterBar.add(showBothButton);

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
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        // TODO should be resizeable with the bottom right corner handle (min 3x1)

        // Lay out components again
        pack();

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    public void refreshHistoryList() {
        historyList.removeAll();
        final File[] files = Ginj.getHistoryFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(Misc.METADATA_EXTENSION));

        if (files == null) {
            UI.alertError(this, "History error", "Could not list files in history folder '" + Ginj.getHistoryFolder().getAbsolutePath() +"'");
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
        starWindow.setHistoryFrame(null);
        // Close window
        dispose();
    }

    private void onEdit(Capture capture) {
        try {
            Capture newCapture = capture.clone();
            newCapture.setVersion(capture.getVersion() + 1);
            newCapture.setOriginalFile(getCaptureFile(capture));
            final CaptureEditingFrame captureEditingFrame = new CaptureEditingFrame(starWindow, newCapture);
            captureEditingFrame.setVisible(true);
        }
        catch (CloneNotSupportedException e) {
            UI.alertException(this, "Clone error", "Error creating clone of previous capture", e);
        }
    }

    private void onExport(Capture capture) {
        // TODO should copy the shared URL back to the clipboard, except for Clipboard that should re-execute an "export"
        UI.featureNotImplementedDialog(this);
    }


    private void onDelete(Capture capture) {
        // TODO ask the question: Also delete from storages (and list them) ?
        // TODO if re-exported captures point to the same source, only delete the source media when it's the last one
        final List<String> sharingCaptures = getCapturesSharingSourceFile(capture);
        String message = "The selected capture will be deleted from the history.\n";
        message += "(For now, the exported version (if any) will remain untouched.)\n";
        if (!sharingCaptures.isEmpty()) {
            message += "NOTE: the source file will remain on disk because it is shared with the following capture(s): " + sharingCaptures + "\n";
        }
        message += "Are you sure you want to delete capture '" + capture.getName() + "'?";
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, message, "Delete Capture", JOptionPane.YES_NO_OPTION)) {
            boolean ok = new File(Ginj.getHistoryFolder(), capture.getBaseFilename() + Misc.METADATA_EXTENSION).delete();
            ok = ok && new File(Ginj.getHistoryFolder(), capture.getBaseFilename() + Misc.THUMBNAIL_EXTENSION).delete();
            if (sharingCaptures.isEmpty()) {
                ok = ok && getCaptureFile(capture).delete();
            }
            if (!ok) {
                UI.alertError(this, "Delete error", "There was an error deleting history files for capture\n" + capture.toString());
            }
            refreshHistoryList();
        }
    }

    private List<String> getCapturesSharingSourceFile(Capture captureToDelete) {
        // Find all other metadata files sharing the same ID
        final File[] metadataFiles = Ginj.getHistoryFolder().listFiles((dir, name) -> name.startsWith(captureToDelete.getId()) && name.endsWith(Misc.METADATA_EXTENSION) && !name.startsWith(captureToDelete.getBaseFilename()));
        List<String> siblingCaptureNames = new ArrayList<>();
        try {
            for (File metadataFile : metadataFiles) {
                try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(metadataFile)))) {
                    Capture siblingCapture = (Capture) xmlDecoder.readObject();
                    siblingCaptureNames.add(siblingCapture.getName());
                }
            }
        }
        catch (Exception e) {
            UI.alertException(this, "Error", "Error determining captures sharing the same file", e);
        }
        return siblingCaptureNames;
    }

    private File getCaptureFile(Capture capture) {
        return new File(Ginj.getHistoryFolder(), capture.getId() + (capture.isVideo() ? Misc.VIDEO_EXTENSION : Misc.IMAGE_EXTENSION));
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
            String xmlFilename = file.getAbsolutePath();

            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(5, 5, 5, 5));

            final JPanel imageLabel = new ThumbnailPanel(xmlFilename.substring(0, xmlFilename.lastIndexOf('.')) + Misc.THUMBNAIL_EXTENSION);
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
                UI.alertException(HistoryFrame.this, "Load error", "Error loading capture '" + file.getAbsolutePath() + "'", e);
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
            File captureFile = new File(xmlFilename.substring(0, xmlFilename.lastIndexOf('.')) + (capture.isVideo()? Misc.VIDEO_EXTENSION : Misc.IMAGE_EXTENSION));
            sizeLabel.setText(Misc.getPrettySize(captureFile.length()));
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
                this.setBackground(UI.HISTORY_SELECTED_ITEM_BACKGROUND_COLOR);
                nameLabel.setForeground(Color.BLACK);
                sizeLabel.setForeground(Color.BLACK);
                editButton.setVisible(true);
                exportButton.setVisible(true);
                deleteButton.setVisible(true);
            }
            else {
                this.setBackground(null);
                nameLabel.setForeground(UI.LABEL_FOREGROUND_COLOR);
                sizeLabel.setForeground(UI.LABEL_FOREGROUND_COLOR);
                editButton.setVisible(false);
                exportButton.setVisible(false);
                deleteButton.setVisible(false);
            }
        }
    }

    private static class ThumbnailPanel extends JPanel {

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
