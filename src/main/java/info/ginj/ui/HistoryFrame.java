package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.Prefs;
import info.ginj.ui.component.BorderedLabel;
import info.ginj.ui.component.HistoryButtonPanel;
import info.ginj.ui.component.HistoryToggleButton;
import info.ginj.ui.component.YellowLabel;
import info.ginj.ui.layout.WrapLayout;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This window displays and manages the historized captures
 */
public class HistoryFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(HistoryFrame.class);

    private final BorderedLabel statusLabel;
    private JScrollPane historyPanel;
    private int numVisibleComponents;

    private enum SortOrder {DATE, SIZE, NAME}
    private enum Filter {IMAGE, VIDEO, BOTH}

    public static final Dimension HISTORY_CELL_SIZE = new Dimension(156, 164);
    public static final Dimension THUMBNAIL_SIZE = new Dimension(113, 91);
    public static final Dimension MAIN_AREA_DEFAULT_SIZE = new Dimension(680, 466);
    public static final Dimension MIN_WINDOW_SIZE = new Dimension(HISTORY_CELL_SIZE.width * 2 + 40, HISTORY_CELL_SIZE.height + 90);
    private final ImageIcon exportIcon;
    private final ImageIcon editIcon;
    private final ImageIcon deleteIcon;

    private final StarWindow starWindow;
    private List<HistoryItemWidget> selectedItems = new ArrayList<>();
    private HistoryItemWidget lastSelectedItem; // To be used for range selection (shift-click)

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
        setTitle(Ginj.getAppName() + " History");
        setIconImage(StarWindow.getAppIcon());


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

        historyList = new JPanel(new WrapLayout(WrapLayout.LEFT));

        historyPanel = new JScrollPane(historyList);
        historyPanel.getVerticalScrollBar().setUnitIncrement(HISTORY_CELL_SIZE.height);
        try {
            int width = Integer.parseInt(Prefs.get(Prefs.Key.HISTORY_WINDOW_WIDTH));
            int height = Integer.parseInt(Prefs.get(Prefs.Key.HISTORY_WINDOW_HEIGHT));
            historyPanel.setPreferredSize(new Dimension(width, height));
        }
        catch (Exception e) {
            historyPanel.setPreferredSize(MAIN_AREA_DEFAULT_SIZE);
        }
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

        ComponentResizer cr = new ComponentResizer();
        // TODO should only be resizeable to min 3x1 cells
        //cr.setSnapSize();
        cr.setMinimumSize(MIN_WINDOW_SIZE);
        cr.registerComponent(this);

        // Lay out components again
        pack();

        addKeyboardShortcuts();

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    private void addKeyboardShortcuts() {
        UI.addEscKeyShortcut(this, e -> onClose());

        getRootPane().registerKeyboardAction(e -> onDeleteCurrentSelection(),
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> onSelectAll(),
                KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
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
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed()); // Sort most recent first

            for (File file : files) {
                historyList.add(new HistoryItemWidget(this, file));
            }
            numVisibleComponents = files.length;
            updateStatusText();
        }
        historyList.validate();
    }

    private void updateStatusText() {
        statusLabel.setText("Captures listed: " + numVisibleComponents + "." + (selectedItems.isEmpty()?"":(" Selected: " + selectedItems.size() + ".")));
    }

    private List<Capture> getCapturesSharingSourceFile(Capture mainCapture) {
        // Find all other metadata files sharing the same ID
        final File[] metadataFiles = Ginj.getHistoryFolder().listFiles((dir, name) -> name.startsWith(mainCapture.getId()) && name.endsWith(Misc.METADATA_EXTENSION) && !name.equals(mainCapture.getBaseFilename() + Misc.METADATA_EXTENSION));
        List<Capture> siblingCaptures = new ArrayList<>();
        try {
            //noinspection ConstantConditions will be caught later
            for (File metadataFile : metadataFiles) {
                try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(metadataFile)))) {
                    Capture siblingCapture = (Capture) xmlDecoder.readObject();
                    siblingCapture.setUp();
                    siblingCaptures.add(siblingCapture);
                }
            }
            return siblingCaptures;
        }
        catch (Exception e) {
            UI.alertException(this, "Error", "Error determining captures sharing the same file", e, logger);
            return null;
        }
    }

    private File getCaptureFile(Capture capture) {
        return new File(Ginj.getHistoryFolder(), capture.getId() + capture.defaultExtension());
    }

    /**
     * To be used upon click
     */
    public void setSelectedItem(HistoryItemWidget item) {
        // Deselect previous ones
        for (HistoryItemWidget previousItem : selectedItems) {
            previousItem.setSelected(false);
        }
        selectedItems.clear();

        // Select new one, if any
        if (item != null) {
            item.setSelected(true);
            selectedItems.add(item);
        }

        // And remember this item for range selection (shift-click)
        lastSelectedItem = item;

        updateStatusText();
    }

    /**
     * To be used upon ctrl-click
     */
    private void toggleSelectedIdem(HistoryItemWidget item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        }
        else {
            selectedItems.add(item);
        }
        item.setSelected(!item.isSelected());

        // And remember for range selection (shift-click)
        lastSelectedItem = item.isSelected()?item:null;

        updateStatusText();
    }


    /**
     * To be used upon shift-[ctrl-]click
     */
    private void extendSelectionToItem(HistoryItemWidget item, boolean addToCurrentSelection) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if (!addToCurrentSelection) {
                // Deselect everything
                for (HistoryItemWidget previousItem : selectedItems) {
                    previousItem.setSelected(false);
                }
                selectedItems.clear();
            }

            // Make sure all items between the last one clicked and this one are selected
            boolean isSelecting = lastSelectedItem == null; // We consider the first elements are outside the range unless there was no previous item selected
            for (Component currentElement : historyList.getComponents()) {
                HistoryItemWidget currentItem;
                try {
                    currentItem = (HistoryItemWidget) currentElement;
                }
                catch (ClassCastException e) {
                    continue;
                }

                if (currentItem.isVisible()) { // In case an image vs video filter is applied
                    if (isSelecting) {
                        currentItem.setSelected(true);
                        selectedItems.add(currentItem);
                    }

                    // See if we are crossing a selection boundary (shift-click must work in both directions)
                    if (currentItem == item || currentItem == lastSelectedItem) {
                        if (isSelecting) {
                            // end of selection
                            break;
                        }
                        // start of selection
                        isSelecting = true;
                        // and select this one before going to the next
                        currentItem.setSelected(true);
                        selectedItems.add(currentItem);
                    }
                }
            }
            updateStatusText();
        }
        finally {
            setCursor(Cursor.getDefaultCursor());
        }

    }


    private void onSelectAll() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            for (Component currentElement : historyList.getComponents()) {
                HistoryItemWidget currentItem;
                try {
                    currentItem = (HistoryItemWidget) currentElement;
                }
                catch (ClassCastException e) {
                    continue;
                }

                if (currentItem.isVisible()) { // In case an image vs video filter is applied
                    currentItem.setSelected(true);
                    if (!selectedItems.contains(currentItem)) {
                        selectedItems.add(currentItem);
                    }
                }
            }
            updateStatusText();
        }
        finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    //////////////////////////////
    // Event handlers

    private void onClose() {
        // Remember size
        Dimension size = historyPanel.getSize();
        Prefs.set(Prefs.Key.HISTORY_WINDOW_WIDTH, String.valueOf(size.width));
        Prefs.set(Prefs.Key.HISTORY_WINDOW_HEIGHT, String.valueOf(size.height));

        starWindow.setHistoryFrame(null);
        // Close window
        dispose();
    }

    private void onEdit(Capture capture) {
        try {
            // Find captures sharing the same source to determine new version (max+1)
            final List<Capture> sharingCaptures = getCapturesSharingSourceFile(capture);
            if (sharingCaptures != null) {
                int maxCaptureVersion = capture.getVersion();
                for (Capture sharingCapture : sharingCaptures) {
                    if (sharingCapture.getVersion() > maxCaptureVersion) maxCaptureVersion = sharingCapture.getVersion();
                }

                // Find if the name ends with a version
                String name = capture.getName();
                final int versionPos = name.lastIndexOf(Capture.VERSION_SEPARATOR);
                if (versionPos > -1) {
                    try {
                        // Check that the end is a number
                        Integer.parseInt(name.substring(versionPos +2));
                        // if it didn't throw an exception, remove the old version from the name
                        name = name.substring(0, versionPos);
                    }
                    catch (NumberFormatException e) {
                        // Keep name unchanged
                    }
                }

                Capture newCapture = capture.clone();
                newCapture.setVersion(maxCaptureVersion + 1);
                newCapture.setName(name + Capture.VERSION_SEPARATOR + (maxCaptureVersion + 1));
                newCapture.setOriginalFile(getCaptureFile(capture));
                final CaptureEditingFrame captureEditingFrame = CaptureEditingFrame.getInstance(starWindow);
                captureEditingFrame.open(newCapture);
            }
        }
        catch (CloneNotSupportedException e) {
            UI.alertException(this, "Clone error", "Error creating clone of previous capture", e, logger);
        }
    }

    private void onExport(Capture capture) {
        // TODO should copy the shared URL back to the clipboard, except for Clipboard that should re-execute an "export"
        UI.featureNotImplementedDialog(this);
    }


    private void onDeleteCurrentSelection() {
        // TODO ask the question: Also delete from storages (and list them) ?
        // TODO if re-exported captures point to the same source, only delete the source media when it's the last one
        String message;
        Capture capture;
        List<Capture> sharingCaptures;
        switch (selectedItems.size()) {
            case 0:
                break;
            case 1:
                message = "The selected capture will be deleted from the history.\n";
                message += "The exported version (if any) will remain untouched.\n";
                capture = selectedItems.get(0).getOrLoadCapture();
                sharingCaptures = getCapturesSharingSourceFile(capture);
                if (sharingCaptures != null) {
                    if (!sharingCaptures.isEmpty()) {
                        message += "(note: the source image will remain on disk because it is shared with the following capture(s): " + sharingCaptures.stream().map(Capture::getName).collect(Collectors.toList()) + ")\n";
                    }
                }
                else {
                    // There was an error finding sharing captures. Putting a dummy capture in the list to prevent deletion of the hi-res file.
                    sharingCaptures = Collections.singletonList(new Capture());
                }
                message += "Are you sure you want to delete capture '" + capture.getName() + "'?";
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, message, "Delete capture", JOptionPane.YES_NO_OPTION)) {
                    deleteCapture(capture, sharingCaptures.isEmpty());
                    loadHistoryList();
                }
                break;
            default:
                message = "All the selected captures will be deleted from the history!\n";
                message += "The exported versions (if any) will remain untouched.\n";
                message += "(note: the source images shared with non-selected captures (if any) will remain on disk)\n";
                message += "Are you sure you want to delete " + selectedItems.size() + " captures?";
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, message, "Delete " + selectedItems.size() + " captures", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
                    for (HistoryItemWidget selectedItem : selectedItems) {
                        capture = selectedItem.getOrLoadCapture();
                        sharingCaptures = getCapturesSharingSourceFile(capture);
                        deleteCapture(capture, sharingCaptures.isEmpty());
                    }
                    loadHistoryList();
                }
        }
    }

    private void deleteCapture(Capture capture, boolean deleteFullSizeImage) {
        boolean ok = new File(Ginj.getHistoryFolder(), capture.getBaseFilename() + Misc.METADATA_EXTENSION).delete();
        ok = ok && new File(Ginj.getHistoryFolder(), capture.getBaseFilename() + Misc.THUMBNAIL_EXTENSION).delete();
        if (deleteFullSizeImage) {
            ok = ok && getCaptureFile(capture).delete();
        }
        if (!ok) {
            UI.alertError(this, "Delete error", "There was an error deleting history files for capture\n" + capture.toString());
        }
    }

    private void onSort(SortOrder order) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            List<Component> components = new ArrayList<>(Arrays.asList(historyList.getComponents()));
            Comparator<Component> comparator = switch (order) {
                case DATE -> (o1, o2) -> {
                    HistoryItemWidget historyItemWidget1 = (HistoryItemWidget) o1;
                    HistoryItemWidget historyItemWidget2 = (HistoryItemWidget) o2;
                    return Long.compare(historyItemWidget2.getFile().lastModified(), historyItemWidget1.getFile().lastModified());
                };
                case SIZE -> (o1, o2) -> {
                    HistoryItemWidget historyItemWidget1 = (HistoryItemWidget) o1;
                    HistoryItemWidget historyItemWidget2 = (HistoryItemWidget) o2;
                    return Long.compare(historyItemWidget2.getCaptureSize(), historyItemWidget1.getCaptureSize());
                };
                case NAME -> (o1, o2) -> {
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

            setSelectedItem(null);
        }
        finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void onFilter(Filter filter) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            numVisibleComponents = 0;
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

            setSelectedItem(null);
        }
        finally {
            setCursor(Cursor.getDefaultCursor());
        }
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
        private boolean selected = false;

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
                capture.setUp();

                nameLabel.setText(capture.getName());
                nameLabel.setToolTipText(capture.getName());
                nameLabel.addMouseListener(new MouseAdapter() {
                    // Trick to keep clickability while showing tooltip, taken from https://stackoverflow.com/a/14932443/13551878
                    public void mouseReleased(MouseEvent e) {
                        // Let clicks pass through to the panel
                        HistoryItemWidget.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, HistoryItemWidget.this));
                    }
                });
                String xmlFilename = file.getAbsolutePath();

                String basename = xmlFilename.substring(0, xmlFilename.lastIndexOf('.'));
                imagePanel.setImagePath(basename + Misc.THUMBNAIL_EXTENSION);
                String exportDetails = "";
                for (Export export : capture.getExports()) {
                    exportDetails += (exportDetails.isEmpty()?"Exported to ":"<br/>Exported to ") + export.toShortString();
                };
                imagePanel.setToolTipText("<html><body>" + exportDetails + "</body></html>");
                UI.restoreMouseBehaviourAfterTooltip(imagePanel);

                int versionPos = basename.lastIndexOf("_v");
                if (versionPos != -1) {
                    basename = basename.substring(0, versionPos);
                }
                File captureFile = new File(basename + capture.defaultExtension());
                captureSize = captureFile.length();
                sizeLabel.setText(Misc.getPrettySize(captureSize));
                addMouseListener(new MouseAdapter() {
                    /**
                     * Note : using "mouseReleased" event instead of "mouseClicked" because "click" does not support the slightest movement between press and release
                     */
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.isControlDown()) {
                            if (e.isShiftDown()) {
                                // System.out.println("Shift-Ctrl-click");
                                historyFrame.extendSelectionToItem(HistoryItemWidget.this, true);
                            }
                            else {
                                // System.out.println("Ctrl-click");
                                historyFrame.toggleSelectedIdem(HistoryItemWidget.this);
                            }
                        }
                        else if (e.isShiftDown()) {
                            // System.out.println("Shift-click");
                            historyFrame.extendSelectionToItem(HistoryItemWidget.this, false);
                        }
                        else {
                            // System.out.println("Plain click");
                            historyFrame.setSelectedItem(HistoryItemWidget.this);
                            if (e.getClickCount() == 2) {
                                onEdit(HistoryItemWidget.this.capture);
                            }
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
                deleteButton.addActionListener(e -> onDeleteCurrentSelection());
            }
            catch (Exception e) {
                UI.alertException(HistoryFrame.this, "Load error", "Error loading capture '" + file.getAbsolutePath() + "'", e, logger);
            }
        }


        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            getOrLoadCapture(); // To make sure all components are initialized
            if (selected) {
                setBackground(UI.HISTORY_SELECTED_ITEM_BACKGROUND_COLOR);
                nameLabel.setForeground(Color.BLACK);
                sizeLabel.setForeground(Color.BLACK);
                editButton.setVisible(true);
                exportButton.setVisible(true);
                deleteButton.setVisible(true);
            }
            else {
                setBackground(null);
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
                    //Logger.info("Loaded " + capture);
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
                logger.error("Error reading '" + imagePath + "'...", e);
            }
            repaint();
        }

        /**
         * Yellow on black tooltip
         * @return
         */
        @Override
        public JToolTip createToolTip() {
            JToolTip tooltip = super.createToolTip();
            tooltip.setBackground(Color.BLACK);
            tooltip.setForeground(UI.LABEL_FOREGROUND_COLOR);
            return tooltip;
        }
    }
}
