package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.model.Capture;
import info.ginj.ui.component.BorderedLabel;
import info.ginj.ui.component.HistoryButtonPanel;
import info.ginj.ui.component.HistoryToggleButton;
import info.ginj.ui.component.YellowLabel;
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
import java.util.Comparator;
import java.util.List;

/**
 * This window displays and manages the historized captures
 */
public class HistoryFrame extends JFrame {

    private final BorderedLabel statusLabel;

    private enum SortOrder {DATE, SIZE, NAME}
    private enum Filter {IMAGE, VIDEO, BOTH}

    public static final Dimension HISTORY_CELL_SIZE = new Dimension(156, 164);
    public static final Dimension THUMBNAIL_SIZE = new Dimension(113, 91);
    public static final Dimension MAIN_AREA_DEFAULT_SIZE = new Dimension(680, 466);
    private final ImageIcon exportIcon;
    private final ImageIcon editIcon;
    private final ImageIcon deleteIcon;

    private final StarWindow starWindow;
    private HistoryItemWidget selectedItem;
    private final JPanel historyList;
    private final HistoryToggleButton sortByDateButton;
    private final HistoryToggleButton filterBothButton;

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
        HistoryButtonPanel buttonBar = new HistoryButtonPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.LINE_AXIS));
        // Force panel height
        buttonBar.add(Box.createRigidArea(new Dimension(0, 27)));

        ButtonGroup sortGroup = new ButtonGroup();

        buttonBar.add(Box.createHorizontalStrut(18));
        sortByDateButton = new HistoryToggleButton("Date");
        sortByDateButton.addActionListener(e -> onSort(SortOrder.DATE));
        sortGroup.add(sortByDateButton);
        buttonBar.add(sortByDateButton);
        buttonBar.add(Box.createHorizontalStrut(8));
        final HistoryToggleButton sortBySizeButton = new HistoryToggleButton("Size");
        sortBySizeButton.addActionListener(e -> onSort(SortOrder.SIZE));
        sortGroup.add(sortBySizeButton);
        buttonBar.add(sortBySizeButton);
        buttonBar.add(Box.createHorizontalStrut(8));
        final HistoryToggleButton sortByNameButton = new HistoryToggleButton("Name");
        sortByNameButton.addActionListener(e -> onSort(SortOrder.NAME));
        sortGroup.add(sortByNameButton);
        buttonBar.add(sortByNameButton);

        buttonBar.add(Box.createHorizontalGlue());

        ButtonGroup filterGroup = new ButtonGroup();

        final HistoryToggleButton filterImageButton = new HistoryToggleButton("Images");
        filterImageButton.addActionListener(e -> onFilter(Filter.IMAGE));
        filterGroup.add(filterImageButton);
        buttonBar.add(filterImageButton);
        buttonBar.add(Box.createHorizontalStrut(8));
        final HistoryToggleButton filterVideoButton = new HistoryToggleButton("Videos");
        filterVideoButton.addActionListener(e -> onFilter(Filter.VIDEO));
        filterGroup.add(filterVideoButton);
        buttonBar.add(filterVideoButton);
        buttonBar.add(Box.createHorizontalStrut(8));
        filterBothButton = new HistoryToggleButton("Both");
        filterBothButton.addActionListener(e -> onFilter(Filter.BOTH));
        filterGroup.add(filterBothButton);
        buttonBar.add(filterBothButton);

        buttonBar.add(Box.createHorizontalStrut(18));

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(buttonBar, c);

        JScrollPane historyPanel;
        historyList = new JPanel(new WrapLayout(WrapLayout.LEFT));

        historyPanel = new JScrollPane(historyList);
        historyPanel.getVerticalScrollBar().setUnitIncrement(HISTORY_CELL_SIZE.height);
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
        statusLabel = new BorderedLabel(" "); // to set height
        statusPanel.add(statusLabel, BorderLayout.WEST);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(statusPanel, c);

        // Load captures
        loadHistoryList();

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        // TODO should be resizeable with the bottom right corner handle (min 3x1)

        // Lay out components again
        pack();

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    public void loadHistoryList() {
        // Default is show all, by date
        sortByDateButton.setSelected(true);
        filterBothButton.setSelected(true);

        historyList.removeAll();
        final File[] files = Ginj.getHistoryFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(Misc.METADATA_EXTENSION));

        if (files == null) {
            UI.alertError(this, "History error", "Could not list files in history folder '" + Ginj.getHistoryFolder().getAbsolutePath() +"'");
            historyList.add(new JLabel("Error"));
        }
        else {
            Arrays.sort(files, (o1, o2) -> (int) (o2.lastModified() - o1.lastModified())); // Sort most recent first

            for (File file : files) {
                historyList.add(new HistoryItemWidget(this, file));
            }
            updateNumCaptures(files.length);
        }
        historyList.validate();
    }

    private void updateNumCaptures(int numVisibleComponents) {
        statusLabel.setText(numVisibleComponents + " captures listed.");
    }

    private List<String> getCapturesSharingSourceFile(Capture captureToDelete) {
        // Find all other metadata files sharing the same ID
        final File[] metadataFiles = Ginj.getHistoryFolder().listFiles((dir, name) -> name.startsWith(captureToDelete.getId()) && name.endsWith(Misc.METADATA_EXTENSION) && !name.startsWith(captureToDelete.getBaseFilename()));
        List<String> siblingCaptureNames = new ArrayList<>();
        try {
            //noinspection ConstantConditions will be caught later
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

    public void setSelectedItem(HistoryItemWidget selectedItem) {
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
    // Event handlers

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
            loadHistoryList();
        }
    }

    private void onSort(SortOrder order) {
        List<Component> components = new ArrayList<>(Arrays.asList(historyList.getComponents()));
        Comparator<Component> comparator = switch (order) {
            case DATE -> (Comparator<Component>) (o1, o2) -> {
                HistoryItemWidget historyItemWidget1 = (HistoryItemWidget) o1;
                HistoryItemWidget historyItemWidget2 = (HistoryItemWidget) o2;
                return (int) (historyItemWidget2.getFile().lastModified() - (historyItemWidget1.getFile().lastModified()));
            };
            case SIZE -> (Comparator<Component>) (o1, o2) -> {
                HistoryItemWidget historyItemWidget1 = (HistoryItemWidget) o1;
                HistoryItemWidget historyItemWidget2 = (HistoryItemWidget) o2;
                return (int) (historyItemWidget2.getCaptureSize() - (historyItemWidget1.getCaptureSize()));
            };
            case NAME -> (Comparator<Component>) (o1, o2) -> {
                HistoryItemWidget historyItemWidget1 = (HistoryItemWidget) o1;
                HistoryItemWidget historyItemWidget2 = (HistoryItemWidget) o2;
                return historyItemWidget1.getOrLoadCapture().getName().compareTo(historyItemWidget2.getOrLoadCapture().getName());
            };
        };
        components.sort(comparator);
        historyList.removeAll();
        for (Component component : components) {
            historyList.add(component);
        }
        historyList.revalidate();
        historyList.repaint();
    }

    private void onFilter(Filter filter) {
        int numVisibleComponents = 0;
        for (Component component : historyList.getComponents()) {
            HistoryItemWidget historyItemWidget = (HistoryItemWidget) component;
            switch (filter) {
                case BOTH -> {
                    historyItemWidget.setVisible(true);
                    numVisibleComponents++;
                }
                case VIDEO -> {
                    final boolean visible = historyItemWidget.getOrLoadCapture().isVideo();
                    historyItemWidget.setVisible(visible);
                    if (visible) numVisibleComponents++;
                }
                case IMAGE -> {
                    final boolean visible = !historyItemWidget.getOrLoadCapture().isVideo();
                    historyItemWidget.setVisible(visible);
                    if (visible) numVisibleComponents++;
                }
            }
        }
        historyList.revalidate();
        historyList.repaint();

        updateNumCaptures(numVisibleComponents);
    }


    //////////////////////////////
    // Inner classes

    private class HistoryItemWidget extends JPanel {
        private final HistoryFrame historyFrame;
        private final File file;

        private Capture capture = null;
        private long captureSize;

        private final JLabel nameLabel;
        private final JLabel sizeLabel;
        private final ThumbnailPanel imagePanel;
        private final JPanel buttonBar;
        private JButton editButton;
        private JButton exportButton;
        private JButton deleteButton;

        public HistoryItemWidget(HistoryFrame historyFrame, File file) {
            super();
            this.historyFrame = historyFrame;
            this.file = file;

            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(5, 5, 5, 5));

            imagePanel = new ThumbnailPanel();
            imagePanel.setBackground(null);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            c.insets = new Insets(10, 10, 10, 10);
            add(imagePanel, c);

            nameLabel = new YellowLabel(file.getName()); // Use filename at first. Will be replaced asynchronously
            nameLabel.setBackground(null);
            nameLabel.setPreferredSize(new Dimension(90, 16));
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            add(nameLabel, c);

            sizeLabel = new YellowLabel("?");
            sizeLabel.setBackground(null);
            sizeLabel.setPreferredSize(new Dimension(55, 16));
            sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.EAST;
            add(sizeLabel, c);

            buttonBar = new JPanel(new GridLayout(1, 3, 5, 0));

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            add(buttonBar, c);

        }

        @Override
        public Dimension getPreferredSize() {
            return HISTORY_CELL_SIZE;
        }

        public long getCaptureSize() {
            getOrLoadCapture(); // Makes sure capture is loaded, which means captureSize has been computed
            return captureSize;
        }

        public File getFile() {
            return file;
        }

        public void loadCapture() {
            try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)))) {
                capture = (Capture) xmlDecoder.readObject();
                nameLabel.setText(capture.getName());
                nameLabel.setToolTipText(capture.getName());
                nameLabel.addMouseListener(new MouseAdapter() {
                    // Trick to keep clickability while showing tooltip, taken from https://stackoverflow.com/a/14932443/13551878
                    public void mouseReleased(MouseEvent e) {
                        // Let clicks pass trhough to the panel
                        HistoryItemWidget.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, HistoryItemWidget.this));
                    }
                });
                String xmlFilename = file.getAbsolutePath();

                imagePanel.setImagePath(xmlFilename.substring(0, xmlFilename.lastIndexOf('.')) + Misc.THUMBNAIL_EXTENSION);

                File captureFile = new File(xmlFilename.substring(0, xmlFilename.lastIndexOf('.')) + (capture.isVideo()? Misc.VIDEO_EXTENSION : Misc.IMAGE_EXTENSION));
                captureSize = captureFile.length();
                sizeLabel.setText(Misc.getPrettySize(captureSize));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        historyFrame.setSelectedItem(HistoryItemWidget.this);
                        if (e.getClickCount() == 2) {
                            onEdit(HistoryItemWidget.this.capture);
                        }
                    }
                });

                buttonBar.setBackground(null);
                buttonBar.setBorder(new EmptyBorder(2, 0, 0, 0));
                editButton = new JButton(editIcon);
                exportButton = new JButton(exportIcon);
                deleteButton = new JButton(deleteIcon);
                // Hide buttons by default
                editButton.setVisible(false);
                exportButton.setVisible(false);
                deleteButton.setVisible(false);
                buttonBar.add(editButton);
                buttonBar.add(exportButton);
                buttonBar.add(deleteButton);

                editButton.addActionListener(e -> onEdit(capture));
                exportButton.addActionListener(e -> onExport(capture));
                deleteButton.addActionListener(e -> onDelete(capture));
            }
            catch (Exception e) {
                UI.alertException(HistoryFrame.this, "Load error", "Error loading capture '" + file.getAbsolutePath() + "'", e);
                e.printStackTrace();
            }
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

        @Override
        public void paint(Graphics g) {
            // Delay loading until display
            getOrLoadCapture();
            super.paint(g);
        }

        public Capture getOrLoadCapture() {
            synchronized (this) { // in case we reorder while loading is in progress
                if (capture == null) {
                    loadCapture();
                    System.out.println("Loaded " + capture);
                }
            }
            return capture;
        }
    }

    private static class ThumbnailPanel extends JPanel {

        private BufferedImage image = null;

        public ThumbnailPanel() {
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
                g.drawString("Image not loaded", 0, THUMBNAIL_SIZE.height/2);
            }
        }

        public void setImagePath(String imagePath) {
            try {
                image = ImageIO.read(new File(imagePath));
            }
            catch (Exception e) {
                System.err.println("Error reading '" + imagePath + "'...");
                e.printStackTrace();
            }
            repaint();
        }
    }
}
