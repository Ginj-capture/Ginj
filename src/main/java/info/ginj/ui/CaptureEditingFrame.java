package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.action.AbstractUndoableAction;
import info.ginj.export.Exporter;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;
import info.ginj.model.Target;
import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.tool.arrow.ArrowTool;
import info.ginj.tool.frame.FrameTool;
import info.ginj.tool.highlight.HighlightTool;
import info.ginj.tool.text.TextTool;
import info.ginj.ui.component.*;
import info.ginj.util.Misc;
import info.ginj.util.UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CaptureEditingFrame extends JFrame implements TargetListChangeListener {

    public static final int TOOL_BUTTON_ICON_WIDTH = 24;
    public static final int TOOL_BUTTON_ICON_HEIGHT = 24;
    public static final int MINI_TOOL_BUTTON_ICON_WIDTH = 10;
    public static final int MINI_TOOL_BUTTON_ICON_HEIGHT = 10;
    public static final Insets MAIN_PANEL_INSETS = new Insets(13, 17, 10, 17);
    public static final Color DEFAULT_TOOL_COLOR = Color.RED;

    // State
    private final StarWindow starWindow;
    private final Capture capture;
    private final ImageEditorPane imagePane;
    private final GinjMiniToolButton undoButton;
    private final GinjMiniToolButton redoButton;
    private final UndoManager undoManager = new UndoManager();
    private final GinjToolButton colorToolButton;
    private final JTextField nameTextField;

    GinjTool currentTool;
    private JPanel actionPanel;


    public CaptureEditingFrame(StarWindow starWindow, BufferedImage capturedImg) {
        this(starWindow, new Capture(new SimpleDateFormat(Misc.DATETIME_FORMAT_PATTERN).format(new Date()), capturedImg)); // ENHANCEMENT: seconds
    }

    public CaptureEditingFrame(StarWindow starWindow, BufferedImage capturedImg, String captureId) {
        this(starWindow, new Capture(captureId, capturedImg));
    }

    public CaptureEditingFrame(StarWindow starWindow, Capture capture) {
        super();
        this.starWindow = starWindow;
        this.capture = capture;

        starWindow.addTargetChangeListener(this);

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " Preview");
        this.setIconImage(StarWindow.getAppIcon());

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);
        // Make it transparent
        setBackground(new Color(0, 0, 0, 0));
        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, this);


        // Prepare main image panel first because it will be needed in ActionHandlers
        BufferedImage originalImage;
        if (capture.isVideo()) {
            throw new RuntimeException("TODO Video");
        }
        else {
            originalImage = capture.getOriginalImage();
            if (originalImage == null) {
                try {
                    originalImage = ImageIO.read(capture.getOriginalFile());
                }
                catch (IOException e) {
                    UI.alertException(this, "Load error", "Error loading capture file '" + capture.getOriginalFile() + "'", e);
                    originalImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
                }
            }
        }
        imagePane = new ImageEditorPane(this, originalImage);


        // Absolute positioning of components over the image
        imagePane.setLayout(null);

        // Restore overlays, if any
        for (Overlay overlay : capture.getOverlays()) {
            imagePane.add(overlay);
        }

        final Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // Prepare title bar
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(UI.getTitleBar("Capture editing" /* to force non-zero height */, null), c);

        // Prepare overlay toolbar
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBorder(new EmptyBorder(6, 6, 6, 6));

        ButtonGroup toolButtonGroup = new ButtonGroup();
        GinjTool[] tools = new GinjTool[]{new ArrowTool(), new TextTool(), new FrameTool(), new HighlightTool()};
        for (GinjTool tool : tools) {
            addToolButton(toolBar, tool, toolButtonGroup);
        }

        colorToolButton = new GinjToolButton(UI.createRoundRectColorIcon(getCurrentColor(), TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT));
        colorToolButton.addActionListener(e -> onColorButtonClick());
        colorToolButton.setToolTipText("Tool Color");
        toolBar.add(colorToolButton);
        toolBar.add(Box.createRigidArea(new Dimension(0, 8)));

        // TODO implement undo
        JPanel undoRedoPanel = new JPanel();
        undoRedoPanel.setAlignmentX(0); // Otherwise the panel adds horizontal space...
        undoRedoPanel.setLayout(new BoxLayout(undoRedoPanel, BoxLayout.X_AXIS));
        undoButton = new GinjMiniToolButton(UI.createIcon(getClass().getResource("/img/icon/undo.png"), MINI_TOOL_BUTTON_ICON_WIDTH, MINI_TOOL_BUTTON_ICON_HEIGHT, UI.TOOLBAR_ICON_ENABLED_COLOR));
        redoButton = new GinjMiniToolButton(UI.createIcon(getClass().getResource("/img/icon/redo.png"), MINI_TOOL_BUTTON_ICON_WIDTH, MINI_TOOL_BUTTON_ICON_HEIGHT, UI.TOOLBAR_ICON_ENABLED_COLOR));

        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> attemptUndo());

        redoButton.setEnabled(false);
        redoButton.addActionListener(e -> attemptRedo());

        undoRedoPanel.add(undoButton);
        undoRedoPanel.add(redoButton);
        toolBar.add(undoRedoPanel);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        contentPane.add(toolBar, c);


        // Prepare an opaque panel which will fill the main display area and host the image scrollpane
        // (the scrollpane will only occupy the center if image is smaller than the toolbars)
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(true);
        mainPanel.setLayout(new GridBagLayout());

        // Let's be confident that the image will fit and we won't need a scrollpane
        // (that will be checked at the end and fixed if needed)
        // Not using a scrollpane guarantees the image will be fully visible, centered, if it is smaller than the available display area
        c = new GridBagConstraints();
        // min border around scrollPane
        c.insets = MAIN_PANEL_INSETS;
        c.weightx = 1;
        c.weighty = 1;
        mainPanel.add(imagePane, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(mainPanel, c);


        // Prepare name editing panel
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BorderLayout());
        final GinjBorderedLabel nameLabel = new GinjBorderedLabel("Name ");
        editPanel.add(nameLabel, BorderLayout.WEST);
        nameTextField = new JTextField();
        editPanel.add(nameTextField, BorderLayout.CENTER);

        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(4, 17, 12, 17);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        lowerPanel.add(editPanel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(lowerPanel, c);


        // Prepare horizontal button bar
        actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actionPanel.add(createExportButtonBar());

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        contentPane.add(actionPanel, c);

        // Prefill and select name
        nameTextField.setText(capture.getBaseFilename());
        nameTextField.selectAll();
        // TODO focus does not work
        nameTextField.requestFocusInWindow();

        /////////////////////////////
        // Check that the window fits on screen, or adjust size and add a scrollpane if needed

        // Lay out components
        pack();

        // Make sure the window fits on the screen:

        // Find the usable screen size (without the Taskbar)
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = env.getMaximumWindowBounds();

        // And compare with current size, choosing the smallest one
        boolean mustResize = false;
        Dimension size = getSize();
        if (size.width > bounds.width) {
            // Must limit width
            size.width = bounds.width;
            mustResize = true;
        }
        if (size.height > bounds.height) {
            // Must limit height
            size.height = bounds.height;
            mustResize = true;
        }

        // See if a resize is needed or not
        if (mustResize) {
            // Replace the imagePane by a JScrollPane filling the whole space and containing in turn the imagePane
            c = new GridBagConstraints();
            // min border around scrollPane
            c.insets = MAIN_PANEL_INSETS;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            JScrollPane scrollableImagePanel = new JScrollPane(imagePane);
            mainPanel.add(scrollableImagePanel, c);
        }
        // Lay out components again
        pack();
        // And limit size
        setSize(size);

        // Center window
        setLocationRelativeTo(null);
    }

    @Override
    public void onTargetListChange() {
        actionPanel.removeAll();
        actionPanel.add(createExportButtonBar());
        revalidate();
    }

    private JPanel createExportButtonBar() {
        JPanel buttonBar = new GinjLowerButtonBar();

        GinjLowerButton shareButton = new GinjLowerButton("Share...", UI.createIcon(getClass().getResource("/img/icon/share.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        shareButton.addActionListener(e -> onShare(shareButton));
        buttonBar.add(shareButton);

        for (Target target : Ginj.getTargetPrefs().getTargetList()) {
            Exporter exporter = target.getExporter();
            if (exporter.isImageSupported() && (!exporter.isOnlineService() || Prefs.isTrue(Prefs.Key.USE_SMALL_BUTTONS_FOR_ONLINE_TARGETS))) {
                GinjLowerButton targetButton = new GinjLowerButton(target.getDisplayName(), exporter.getButtonIcon(16));
                targetButton.addActionListener(e -> onExport(target));
                buttonBar.add(targetButton);
            }
        }

        final JButton cancelButton = new GinjLowerButton("Cancel", UI.createIcon(getClass().getResource("/img/icon/cancel.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        cancelButton.addActionListener(e -> onCancel());
        buttonBar.add(cancelButton);

        // Do we restore this button ?
//        final JButton customizeButton = new GinjLowerButton("Customize Ginj buttons", UI.createIcon(getClass().getResource("/img/icon/customize.png"), 16, 16, UI.ICON_ENABLED_COLOR));
//        customizeButton.addActionListener(e -> onCustomize());
//        buttonBar.add(customizeButton);
        return buttonBar;
    }

    public Color getCurrentColor() {
        Color color = Prefs.getColorWithSuffix(Prefs.Key.TOOL_COLOR_PREFIX, currentTool.getName());
        if (color != null) {
            return color;
        }
        else {
            return DEFAULT_TOOL_COLOR;
        }
    }

    public void setCurrentColor(Color currentColor) {
        Prefs.setColorWithSuffix(Prefs.Key.TOOL_COLOR_PREFIX, currentTool.getName(), currentColor);
        updateColorButtonIcon();
        imagePane.setColorOfSelectedOverlay(currentColor);
    }

    public void updateColorButtonIcon() {
        colorToolButton.setIcon(UI.createRoundRectColorIcon(getCurrentColor(), TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT));
    }

    private void onColorButtonClick() {
        // Create popup color selection window
        final ColorSelectorWindow colorSelectorWindow = new ColorSelectorWindow(this);
        // Position it under the button
        final Point colorButtonLocationOnScreen = colorToolButton.getLocationOnScreen();
        colorSelectorWindow.setLocation(colorButtonLocationOnScreen.x, colorButtonLocationOnScreen.y + colorToolButton.getHeight());
        // And show it
        colorSelectorWindow.setVisible(true);
    }

    private void addToolButton(JPanel toolBar, GinjTool tool, ButtonGroup group) {
        // Create button
        GinjToolToggleButton toolButton = new GinjToolToggleButton(UI.createIcon(
                getClass().getResource("/img/icon/tool_" + tool.getName().toLowerCase() + ".png"),
                TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT, UI.TOOLBAR_ICON_ENABLED_COLOR));
        // Add it to the toolbar, followed by a spacer
        toolButton.setToolTipText(tool.getName());
        toolBar.add(toolButton);
        toolBar.add(Box.createRigidArea(new Dimension(0, 8)));

        // Add the button to the buttonGroup to get a "radio-button" experience
        group.add(toolButton);

        // Select this button if it's the first
        if (currentTool == null) {
            toolButton.setSelected(true);
            currentTool = tool;
        }

        // And store this tool as currentTool if clicked
        toolButton.addActionListener((event) -> {
            currentTool = tool;
            imagePane.setSelectedOverlay(null);
            updateColorButtonIcon();
        });
    }

    private void refreshUndoRedoButtons() {
        undoButton.setEnabled(undoManager.canUndo());
        redoButton.setEnabled(undoManager.canRedo());
        // ENHANCEMENT
        undoButton.setToolTipText(undoManager.canUndo() ? undoManager.getUndoPresentationName() : null);
        redoButton.setToolTipText(undoManager.canRedo() ? undoManager.getRedoPresentationName() : null);
    }

    public void attemptUndo() {
        if (undoManager.canUndo()) {
            try {
                undoManager.undo();
            }
            catch (CannotRedoException cre) {
                cre.printStackTrace();
            }
            imagePane.repaint();
            refreshUndoRedoButtons();
        }
    }

    public void attemptRedo() {
        if (undoManager.canRedo()) {
            try {
                undoManager.redo();
            }
            catch (CannotRedoException cre) {
                cre.printStackTrace();
            }
            imagePane.repaint();
            refreshUndoRedoButtons();
        }
    }

    /**
     * Add a custom action on overlays to the Undo stack
     * <p>
     *
     * @param action The undoable action
     */
    public void addUndoableAction(AbstractUndoableAction action) {
        //System.out.println("Adding undoable action: " + action.getPresentationName());
        undoManager.undoableEditHappened(new UndoableEditEvent(imagePane, action));
        refreshUndoRedoButtons();
    }

    /**
     * Add a standard edit in a textArea to the Undo stack
     * <p>
     *
     * @param edit The undoable edit
     */
    public void addUndoableEdit(UndoableEdit edit) {
        undoManager.addEdit(edit);
        refreshUndoRedoButtons();
    }


    private void onExport(Target target) {
        // Render image and overlays, but no handles
        imagePane.setSelectedOverlay(null);
        BufferedImage renderedImage = new BufferedImage(imagePane.getWidth(), imagePane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = renderedImage.getGraphics();
        imagePane.paint(g);
        g.dispose();
        capture.setRenderedImage(renderedImage);

        // Save name and overlays
        capture.setName(nameTextField.getText());

        List<Overlay> overlays = new ArrayList<>();
        for (Component component : imagePane.getComponents()) {
            if (component instanceof Overlay) {
                overlays.add((Overlay) component);
            }
        }
        capture.setOverlays(overlays);

        // Perform export
        Exporter exporter = target.getExporter();
        ExportFrame exportFrame = new ExportFrame(this, capture, exporter);
        exporter.initialize(this, exportFrame);
        // Note the chicken/egg problem:
        // - Frame needs the Exporter to start and control it
        // - Exporter needs the Frame to update UI (progress and message)
        // There's a risk of a circular reference preventing GC, that's why all exit points of the ExportFrame set the exporter field to null

        exportFrame.setVisible(true);

        if (exportFrame.startExport(target)) {
            // Hide this window during export. It will be "re-opened" in case of failure or cancellation
            setVisible(false);
        }
    }

    private void onCancel() {
        // Close window
        dispose();
    }

    private void onShare(GinjLowerButton button) {
        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();

        JMenuItem menuItem;
        for (Target target : Ginj.getTargetPrefs().getTargetList()) {
            Exporter exporter = target.getExporter();
            if (exporter.isOnlineService()) {
                menuItem = new JMenuItem(target.getDisplayName(), exporter.getButtonIcon(24));
                menuItem.addActionListener(e -> onExport(target));
                popup.add(menuItem);
            }
        }

        menuItem = new JMenuItem("Manage targets...", UI.createIcon(getClass().getResource("/img/icon/share.png"), 24, 24));
        menuItem.addActionListener(e -> onConfigureTargets());
        popup.add(menuItem);

        popup.show(button, button.getWidth() / 2, button.getHeight() / 2);
    }

    private void onConfigureTargets() {
        if (starWindow.getTargetManagementFrame() == null) {
            starWindow.setTargetManagementFrame(new TargetManagementFrame(starWindow));
        }
        starWindow.getTargetManagementFrame().setVisible(true);
        starWindow.getTargetManagementFrame().requestFocus();
    }

    private void onCustomize() {
        // TODO
    }

    @Override
    public void dispose() {
        starWindow.removeTargetChangeListener(this);
        super.dispose();
    }

}
