package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.action.AbstractUndoableAction;
import info.ginj.export.ExportContext;
import info.ginj.export.Exporter;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;
import info.ginj.model.Target;
import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.ui.component.*;
import info.ginj.util.Jaffree;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static info.ginj.ui.component.BoundedTimelineRangeModel.THUMB_NONE;

public class CaptureEditingFrame extends JFrame implements TargetListChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(CaptureEditingFrame.class);

    public static final int TOOL_BUTTON_ICON_WIDTH = 24;
    public static final int TOOL_BUTTON_ICON_HEIGHT = 24;
    public static final int MINI_TOOL_BUTTON_ICON_WIDTH = 10;
    public static final int MINI_TOOL_BUTTON_ICON_HEIGHT = 10;
    public static final Insets MAIN_PANEL_INSETS = new Insets(13, 17, 10, 17);
    public static final Color DEFAULT_TOOL_COLOR = Color.RED;

    // State
    private final StarWindow starWindow;
    Capture capture;
    private ImageEditorPane imagePane;
    private MiniToolButton undoButton;
    private MiniToolButton redoButton;
    private UndoManager undoManager;
    private ToolButton colorToolButton;
    private JTextField nameTextField;

    GinjTool currentTool;
    private JPanel actionPanel;
    private Timer videoImageUpdateTimer = null;
    private int displayedVideoImagePositionMs = 0;
    // Delay of slider pause between image refresh
    private int videoImageUpdateMs;
    private JTimelineSlider positionSlider = null;
    private boolean available;


    public CaptureEditingFrame(StarWindow starWindow) {
        super();
        this.starWindow = starWindow;

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
    }

    public void open(Capture capture) {
        this.capture = capture;

        starWindow.addTargetChangeListener(this);

        undoManager = new UndoManager();

        videoImageUpdateMs = Prefs.getAsInt(Prefs.Key.VIDEO_IMAGE_UPDATE_DELAY_MS);

        // Prepare main image panel first because it will be needed in ActionHandlers
        BufferedImage originalImage;
        if (capture.isVideo()) {
            originalImage = Jaffree.grabImage(capture.getOriginalFile(), capture.getVideoLowerBoundMs());

            // For video playback, JavaFX could be an option...:
            // (from https://stackoverflow.com/questions/52038982/how-to-play-mp4-video-in-java-swing-app )
//            final JFXPanel VFXPanel = new JFXPanel();
//
//            File video_source = new File("tutorial.mp4");
//            Media m = new Media(video_source.toURI().toString());
//            MediaPlayer player = new MediaPlayer(m);
//            MediaView viewer = new MediaView(player);
//
//            StackPane root = new StackPane();
//            Scene scene = new Scene(root);
//
//            // center video position
//            javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
//            viewer.setX((screen.getWidth() - videoPanel.getWidth()) / 2);
//            viewer.setY((screen.getHeight() - videoPanel.getHeight()) / 2);
//
//            // resize video based on screen size
//            DoubleProperty width = viewer.fitWidthProperty();
//            DoubleProperty height = viewer.fitHeightProperty();
//            width.bind(Bindings.selectDouble(viewer.sceneProperty(), "width"));
//            height.bind(Bindings.selectDouble(viewer.sceneProperty(), "height"));
//            viewer.setPreserveRatio(true);
//
//            // add video to stackpane
//            root.getChildren().add(viewer);
//
//            VFXPanel.setScene(scene);
//            videoPanel.setLayout(new BorderLayout());
//            videoPanel.add(VFXPanel, BorderLayout.CENTER);
//            player.play();

        }
        else {
            originalImage = capture.getOriginalImage();
            if (originalImage == null) {
                try {
                    originalImage = ImageIO.read(capture.getOriginalFile());
                }
                catch (IOException e) {
                    UI.alertException(this, "Load error", "Error loading capture file '" + capture.getOriginalFile() + "'", e, logger);
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
        contentPane.add(UI.getTitleBar(" " /* to force non-zero height */, null), c);

        // Prepare overlay toolbar
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBorder(new EmptyBorder(6, 6, 6, 6));

        ButtonGroup toolButtonGroup = new ButtonGroup();
        Set<GinjTool> tools = Prefs.getToolSet();
        for (GinjTool tool : tools) {
            addToolButton(toolBar, tool, toolButtonGroup);
        }

        colorToolButton = new ToolButton(UI.createRoundRectColorIcon(getCurrentColor(), TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT));
        colorToolButton.addActionListener(e -> onColorButtonClick());
        colorToolButton.setToolTipText("Tool Color");
        toolBar.add(colorToolButton);
        toolBar.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel undoRedoPanel = new JPanel();
        undoRedoPanel.setAlignmentX(0); // Otherwise the panel adds horizontal space...
        undoRedoPanel.setLayout(new BoxLayout(undoRedoPanel, BoxLayout.X_AXIS));
        undoButton = new MiniToolButton(UI.createIcon(getClass().getResource("/img/icon/undo.png"), MINI_TOOL_BUTTON_ICON_WIDTH, MINI_TOOL_BUTTON_ICON_HEIGHT, UI.TOOLBAR_ICON_ENABLED_COLOR));
        redoButton = new MiniToolButton(UI.createIcon(getClass().getResource("/img/icon/redo.png"), MINI_TOOL_BUTTON_ICON_WIDTH, MINI_TOOL_BUTTON_ICON_HEIGHT, UI.TOOLBAR_ICON_ENABLED_COLOR));

        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> attemptUndo());

        redoButton.setEnabled(false);
        redoButton.addActionListener(e -> attemptRedo());

        undoRedoPanel.add(undoButton);
        undoRedoPanel.add(redoButton);
        toolBar.add(undoRedoPanel);

        if (!capture.isVideo()) {
            // TODO enable overlays on video
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            contentPane.add(toolBar, c);
        }

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

        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new GridBagLayout());

        // If video, prepare "transport" panel
        if (capture.isVideo()) {
            JPanel transportPanel = new JPanel();
            transportPanel.setLayout(new BorderLayout());
            final BorderedLabel positionLabel = new BorderedLabel("00:00:00");
            transportPanel.add(positionLabel, BorderLayout.WEST);

            // Prepare slider. Note: typecasts limit videos to 600 hours. Should be enough :-)
            positionSlider = new JTimelineSlider(JSlider.HORIZONTAL, 0,
                    (int) capture.getVideoDurationMs(),
                    (int) capture.getVideoLowerBoundMs(),
                    (int) capture.getVideoHigherBoundMs(),
                    (int) capture.getVideoLowerBoundMs());
            positionSlider.setMajorTickSpacing(1000);
            transportPanel.add(positionSlider, BorderLayout.CENTER);
            positionSlider.addChangeListener(e -> {
                JTimelineSlider source = (JTimelineSlider)e.getSource();
                Duration position = Duration.ofMillis(positionSlider.getValue());
                positionLabel.setText(String.format("%02d:%02d:%02d", position.toHours(), position.toMinutesPart(), position.toSecondsPart()));
                if (source.getAdjustingThumbIndex() != THUMB_NONE) {
                    // During drag, wait for value to settle
                    if (videoImageUpdateTimer == null) {
                        videoImageUpdateTimer = new Timer(videoImageUpdateMs, e1 -> updateVideoImage(capture, positionSlider.getThumbValue(positionSlider.getAdjustingThumbIndex())));
                    }
                    else {
                        videoImageUpdateTimer.stop();
                    }
                    videoImageUpdateTimer.start();
                }
                else {
                    if (videoImageUpdateTimer != null) {
                        videoImageUpdateTimer.stop();
                        videoImageUpdateTimer = null;
                        updateVideoImage(capture, positionSlider.getValue());
                    }
                }
            });

            Duration totalDuration = Duration.ofMillis(capture.getVideoDurationMs());
            final BorderedLabel durationLabel = new BorderedLabel(String.format("%02d:%02d:%02d", totalDuration.toHours(), totalDuration.toMinutesPart(), totalDuration.toSecondsPart()));
            transportPanel.add(durationLabel, BorderLayout.EAST);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = GridBagConstraints.RELATIVE;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(8, 17, 8, 17);
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            lowerPanel.add(transportPanel, c);
        }

        // Prepare name editing panel
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BorderLayout());
        final BorderedLabel nameLabel = new BorderedLabel("Name ");
        editPanel.add(nameLabel, BorderLayout.WEST);
        nameTextField = new JTextField();
        editPanel.add(nameTextField, BorderLayout.CENTER);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.gridheight = 1;
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
        if (capture.getName() != null && capture.getName().length() > 0) {
            nameTextField.setText(capture.getName());
        }
        else {
            nameTextField.setText(capture.getBaseFilename());
        }
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

        UI.addEscKeyShortcut(this, e -> onCancel());

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);

        setVisible(true);
    }

    public void close() {
        logger.debug("CaptureEditingFrame.close");
        // Will allow garbage collection of the content pane's subcomponents
        getContentPane().removeAll();
        // and of the capture and attached objects.
        capture = null;

        setVisible(false);

        UI.removeEscKeyShortcut(this);

        starWindow.removeTargetChangeListener(this);

        if (videoImageUpdateTimer != null) {
            videoImageUpdateTimer.stop();
            videoImageUpdateTimer = null;
        }

        // free all references so they are garbage collected
        imagePane = null;
        colorToolButton = null;
        undoButton = null;
        redoButton = null;
        positionSlider = null;
        nameTextField = null;
        actionPanel = null;

        // "Return this window to the pool"
        available = true;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    private void updateVideoImage(Capture capture, int positionInMillis) {
        if (positionInMillis != displayedVideoImagePositionMs) {
            // update image
            imagePane.setCapturedImg(Jaffree.grabImage(capture.getOriginalFile(), positionInMillis));
            imagePane.invalidate();
            imagePane.repaint();
            displayedVideoImagePositionMs = positionInMillis;
        }
    }

    @Override
    public void onTargetListChange() {
        actionPanel.removeAll();
        actionPanel.add(createExportButtonBar());
        revalidate();
    }

    private JPanel createExportButtonBar() {
        JPanel buttonBar = new LowerButtonBar();

        if (!Prefs.isTrue(Prefs.Key.USE_SMALL_BUTTONS_FOR_ONLINE_TARGETS)) {
            LowerButton shareButton = new LowerButton("Share...", UI.createIcon(getClass().getResource("/img/icon/share.png"), 16, 16, UI.ICON_ENABLED_COLOR));
            shareButton.addActionListener(e -> onShare(shareButton));
            buttonBar.add(shareButton);
        }

        for (Target target : Ginj.getTargetPrefs().getTargetList()) {
            Exporter exporter = target.getExporter();
            if (
                    ((capture.isVideo() && exporter.isVideoSupported()) || (!capture.isVideo() && exporter.isImageSupported()))
                    && (!exporter.isOnlineService() || Prefs.isTrue(Prefs.Key.USE_SMALL_BUTTONS_FOR_ONLINE_TARGETS))
            ) {
                LowerButton targetButton = new LowerButton(target.getDisplayName(), exporter.getButtonIcon(16));
                targetButton.addActionListener(e -> onExport(target));
                buttonBar.add(targetButton);
            }
        }

        final JButton cancelButton = new LowerButton("Cancel", UI.createIcon(getClass().getResource("/img/icon/cancel.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        cancelButton.addActionListener(e -> onCancel());
        buttonBar.add(cancelButton);

        // Do we restore this button ?
//        final JButton customizeButton = new LowerButton("Customize Ginj buttons", UI.createIcon(getClass().getResource("/img/icon/customize.png"), 16, 16, UI.ICON_ENABLED_COLOR));
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
        ToolToggleButton toolButton = new ToolToggleButton(UI.createIcon(
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
            catch (CannotUndoException e) {
                logger.error("Cannot undo", e);
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
            catch (CannotRedoException e) {
                logger.error("Cannot redo", e);
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
        //Logger.info("Adding undoable action: " + action.getPresentationName());
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
        // 1. Render image and overlays, but no handles
        imagePane.setSelectedOverlay(null);
        if (capture.isVideo()) {
            capture.setVideoLowerBoundMs(positionSlider.getLower());
            capture.setVideoHigherBoundMs(positionSlider.getHigher());
        }
        else {
            BufferedImage renderedImage = new BufferedImage(imagePane.getWidth(), imagePane.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = renderedImage.createGraphics();
            imagePane.paint(g);
            g.dispose();
            capture.setRenderedImage(renderedImage);
        }

        // Save name and overlays
        capture.setName(nameTextField.getText());

        List<Overlay> overlays = new ArrayList<>();
        for (Component component : imagePane.getComponents()) {
            if (component instanceof Overlay) {
                overlays.add((Overlay) component);
            }
        }
        capture.setOverlays(overlays);

        // 2. Perform export
        Exporter exporter = target.getExporter();

        // Risk of memory leak: https://stackoverflow.com/questions/39437481/jframe-is-never-garbage-collected
        // ExportFrame should not hold any field
        logger.debug("Creating exportFrame");
        ExportFrame exportFrame = new ExportFrame(this);

        logger.debug("Preparing exporter");
        ExportContext exportContext = exporter.prepare(this, starWindow, exportFrame, capture, target);
        if (exportContext != null) {
            Thread exportThread = new Thread(() -> {
                logger.debug("ExportThread: renderCapture");
                renderCapture(capture);
                logger.debug("ExportThread: exportCapture");
                exporter.exportCapture(exportContext, capture, target);
                logger.debug("ExportThread: done.");
                // Close
                close();
            });
            logger.debug("Starting export thread");
            exportThread.start();
            logger.debug("Hiding capture window");
            setVisible(false);
        }
        else {
            logger.info("Export aborted");
            logger.debug("Closing this window");
            exportFrame.close();
        }
        logger.debug("CaptureEditingFrame.onExport returning");
    }

    private void renderCapture(Capture capture) {
        if (capture.isVideo()) {
            // By default, just point to the original file
            File renderedFile = capture.getOriginalFile();

            if (capture.getVideoLowerBoundMs() > 0 || capture.getVideoHigherBoundMs() < capture.getVideoDurationMs()) {
                // TODO should also "render" video file if it has overlays
                renderedFile = new File(Ginj.getTempDir(), capture.getId() + "_trim" +  Misc.VIDEO_EXTENSION);
                Jaffree.trim(capture.getOriginalFile(), capture.getVideoLowerBoundMs(), capture.getVideoHigherBoundMs(), renderedFile);
            }

            capture.setRenderedFile(renderedFile);
        }
    }

    private void onCancel() {
        // Close window
        close();
    }

    private void onShare(LowerButton button) {
        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();

        JMenuItem menuItem;
        for (Target target : Ginj.getTargetPrefs().getTargetList()) {
            Exporter exporter = target.getExporter();
            if (
                    ((capture.isVideo() && exporter.isVideoSupported()) || (!capture.isVideo() && exporter.isImageSupported()))
                            && (exporter.isOnlineService())
            ) {
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
        starWindow.openTargetManagementFrame();
    }

    // Window pool management
    // We reuse "CaptureEditingFrame" windows, because creating and disposing them causes a memory leak.
    // See: https://stackoverflow.com/questions/39437481/jframe-is-never-garbage-collected
    // However, several can be open at the same time, so we're using a pool
    // TODO Now that we clean up the CaptureEditingFrame after use, the mem leak is probably negligible

    private static Set<CaptureEditingFrame> captureEditingFramePool = new HashSet<>();

    public synchronized static CaptureEditingFrame getInstance(StarWindow starWindow) {
        for (CaptureEditingFrame captureEditingFrame : captureEditingFramePool) {
            if (captureEditingFrame.isAvailable()) {
                captureEditingFrame.setAvailable(false);
                return captureEditingFrame;
            }
        }
        // If we came here, no available capture editing frame is available in the pool
        // Create a new one
        final CaptureEditingFrame captureEditingFrame = new CaptureEditingFrame(starWindow);
        captureEditingFramePool.add(captureEditingFrame);

        // and return it
        captureEditingFrame.setAvailable(false);
        return captureEditingFrame;
    }

}
