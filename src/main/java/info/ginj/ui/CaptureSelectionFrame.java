package info.ginj.ui;

import com.github.kokorin.jaffree.OS;
import info.ginj.Ginj;
import info.ginj.jna.DisplayInfo;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;
import info.ginj.ui.component.BorderedLabel;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.ui.component.LowerButton;
import info.ginj.ui.component.LowerButtonBar;
import info.ginj.util.Coords;
import info.ginj.util.Jaffree;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the full screen window on which the area selection is made.
 * It takes up the full screen, captures the screen on startup and uses it as background, then paints the selection box
 * above it.
 * Note: an undecorated JFrame is required instead of a JWindow, otherwise keyboard events (ESC) are not captured
 * <p>
 * Note about multiscreen systems and coordinates:
 * The "main display" always has (0,0) at the top left of the "client area" (where normal windows should be drawn).
 * If secondary displays are positioned to the right and/or below it, all their points are in the (+,+) quadrant,
 * but if a secondary display is above or to the left, one or both of its coordinates will be negative.
 * The logic is that all physical displays' bounds are first union'ed to create a mega-rectangle encompassing all displays.
 * Then, that mega-rectangle is captured to create a mega-image.
 * Finally, a mega-window is created and displayed at the top-left corner of the mega-rectangle (maybe in negative space)
 * and painted with the mega-image, so it gives the impression you're selecting part of the screen while in fact
 * you're selecting part of the already captured image.
 * Note that it is possible that the top left corner in question is outside of all displays (e.g. the leftmost display
 * could be a small one with its bottom aligned with a large central one)
 * Anyway, inside that mega-window, the whole image is in a positive coordinate system, so an offset must be applied
 * to the mouse coordinates to draw the correct selection.
 * That also means there is a bunch of math and trial and error involved to compute the best place to pop-up
 * the button panel once the mouse button is released, as the panel *must* be visible of course.
 * Thanks for reading :-)
 */
public class CaptureSelectionFrame extends AbstractAllDisplaysFrame {

    private static final Logger logger = LoggerFactory.getLogger(CaptureSelectionFrame.class);

    public static final int CURSOR_BOX_MIN_WIDTH = 75;
    public static final int CURSOR_BOX_HEIGHT = 18;
    public static final int CURSOR_BOX_OFFSET = 8;
    public static final int CURSOR_BOX_MARGIN_WIDTH = 5;

    private static final int RESIZE_AREA_IN_MARGIN = 5;
    private static final int RESIZE_AREA_OUT_MARGIN = 10;

    private static final int OPERATION_NONE = -1;
    public static final int SELECTED_AREA_STROKE_WIDTH = 2;
    public static final BasicStroke SELECTED_AREA_STROKE = new BasicStroke(SELECTED_AREA_STROKE_WIDTH);

    // Caching
    // See https://stackoverflow.com/a/10687248
    private final Cursor CURSOR_NONE = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), null);


    // Current state
    private BufferedImage capturedScreenImg;
    private Point rememberedReferenceOffset = null; // filled when selecting or dragging
    private int currentOperation = OPERATION_NONE;
    private boolean isInitialSelectionDone;
    private boolean isShiftDown = false;

    private BorderedLabel captureSizeLabel;
    private JButton imageButton;
    private JButton videoButton;

    public CaptureSelectionFrame(StarWindow starWindow) {
        super(starWindow, Ginj.getAppName() + " Selection");

        addKeyboardBehaviour();

        addMouseBehaviour();

        resetSelection();
    }

    protected JPanel createActionPanel() {
        JPanel actionPanel = new DoubleBorderedPanel(); // To add a margin around buttonBar
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 2));
        JPanel buttonBar = new LowerButtonBar();

        imageButton = new LowerButton("Capture image", UI.createIcon(getClass().getResource("/img/icon/image.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        imageButton.addActionListener(e -> onCaptureImage());
        buttonBar.add(imageButton);
        videoButton = new LowerButton("Capture video", UI.createIcon(getClass().getResource("/img/icon/video.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        videoButton.addActionListener(e -> onCaptureVideo());
        buttonBar.add(videoButton);
        final JButton redoButton = new LowerButton("Redo selection", UI.createIcon(getClass().getResource("/img/icon/redo_selection.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        redoButton.addActionListener(e -> onRedo());
        buttonBar.add(redoButton);
        final JButton cancelButton = new LowerButton("Cancel", UI.createIcon(getClass().getResource("/img/icon/cancel.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        cancelButton.addActionListener(e -> onCancel());
        buttonBar.add(cancelButton);
        captureSizeLabel = new BorderedLabel("9999 x 99999");
        buttonBar.add(captureSizeLabel);

        actionPanel.add(buttonBar);
        return actionPanel;
    }

    protected CaptureMainPane createContentPane() {
        return new CaptureMainPane();
    }


    public class CaptureMainPane extends JPanel {
        // Caching
        private final Image dimmedScreenImg;
        private Font font;
        private FontRenderContext fontRenderContext;

        public CaptureMainPane() {
            try {
                Robot robot = new Robot();

// Simulate small screen to be able to debug in parallel of "full screen" capture window on top
//capturedArea = new Rectangle(0,0,800,600);
//visibleAreas.clear();
//visibleAreas.add(capturedArea);
                logger.info("Free memory: about "
                        // See https://stackoverflow.com/a/12807848/13551878
                        + (Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()))
                        + "bytes. Capturing area: " + allDisplaysBounds);

                capturedScreenImg = robot.createScreenCapture(allDisplaysBounds);
            }
            catch (AWTException e) {
                logger.error("Error performing robot capture", e);
            }

            // Prepared a dimmed & greyscale version to be used for "unselected area"
            dimmedScreenImg = UI.makeDimmedImage(capturedScreenImg);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(allDisplaysBounds.width, allDisplaysBounds.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            Point mousePosition;
            if (OS.IS_WINDOWS && Prefs.isTrue(Prefs.Key.USE_JNA_FOR_WINDOWS_MONITORS)) {
                mousePosition = DisplayInfo.getMousePosition();
            }
            else {
                mousePosition = getMousePosition();
            }
            if (mousePosition != null) {
                // If the bounds extend to negative coordinates, fix the returned (always >0) returned position
                mousePosition.translate(-allDisplaysBounds.x, -allDisplaysBounds.y);
            }

            Rectangle rectangleToDraw = null;

            // Determine rectangle to draw (if any)
            if (selection != null) {
                // Selection done
                rectangleToDraw = selection;
            }

            if (rectangleToDraw != null) {
                // Draw the dimmed image as background
                g2d.drawImage(dimmedScreenImg, 0, 0, this);

                // Draw part of the original image over the dimmed image
                g2d.setClip(rectangleToDraw);
                g2d.drawImage(capturedScreenImg, 0, 0, this);
                g2d.setClip(0, 0, allDisplaysBounds.width, allDisplaysBounds.height);

                // Draw the selection rectangle
                g2d.setColor(UI.AREA_SELECTION_COLOR);
                g2d.setStroke(SELECTED_AREA_STROKE);
                g2d.drawRect(rectangleToDraw.x, rectangleToDraw.y, rectangleToDraw.width, rectangleToDraw.height);
            }
            else {
                // Draw the non-dimmed image on the whole screen
                g2d.drawImage(capturedScreenImg, 0, 0, this);
            }

            if (!isInitialSelectionDone && mousePosition != null) {
                // First selection in progress

                // Draw cross lines
                g2d.setColor(UI.AREA_SELECTION_COLOR);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(mousePosition.x, allDisplaysBounds.y, mousePosition.x, (int) allDisplaysBounds.getHeight());
                g2d.drawLine(allDisplaysBounds.x, mousePosition.y, (int) allDisplaysBounds.getWidth(), mousePosition.y);
            }

            // Determine cursorText to print in size box
            String cursorText = null;
            if (selection == null) {
                if (isShiftDown && mousePosition != null) {
                    cursorText = mousePosition.x + "," + mousePosition.y;
                    Point jnaPosition = DisplayInfo.getMousePosition();
                    if (jnaPosition != null) {
                        cursorText += " / " + jnaPosition.x + "," + jnaPosition.y;
                    }
                }
                else {
                    if (rectangleToDraw == null) {
                        // No (partial) selection yet, show screen size
                        // TODO : "capturedArea" to be replaced by "hovered window" when window detection is implemented
                        cursorText = allDisplaysBounds.width + " x " + allDisplaysBounds.height;
                    }
                    else {
                        // We're dragging, show current size
                        cursorText = rectangleToDraw.width + " x " + rectangleToDraw.height;
                    }
                }
            }
            else if (currentOperation != OPERATION_NONE && currentOperation != Cursor.DEFAULT_CURSOR && currentOperation != Cursor.MOVE_CURSOR) {
                cursorText = selection.width + " x " + selection.height;
            }

            if (cursorText != null && mousePosition != null) {
                // Use antialiasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Prepare font
                if (font == null) {
                    font = g2d.getFont();
                    fontRenderContext = g2d.getFontRenderContext();
                    Map<TextAttribute, Object> attributes = new HashMap<>();
                    attributes.put(TextAttribute.FAMILY, font.getFamily());
                    attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD);
                    attributes.put(TextAttribute.SIZE, font.getSize() * 0.9);
                    font = Font.getFont(attributes);
                }
                g2d.setFont(font);

                // Compute text size
                int textWidth = (int) font.getStringBounds(cursorText, fontRenderContext).getWidth();
                LineMetrics ln = font.getLineMetrics(cursorText, fontRenderContext);
                int textHeight = (int) (ln.getAscent() + ln.getDescent());

                // Compute the cursor text box
                int cursorBoxWidth = Integer.max(CURSOR_BOX_MIN_WIDTH, textWidth + 2 * CURSOR_BOX_MARGIN_WIDTH);
                int cursorBoxX = mousePosition.x + CURSOR_BOX_OFFSET;
                if (cursorBoxX + cursorBoxWidth > allDisplaysBounds.width) {
                    cursorBoxX = mousePosition.x - CURSOR_BOX_OFFSET - cursorBoxWidth;
                }
                int cursorBoxY = mousePosition.y + CURSOR_BOX_OFFSET;
                if (cursorBoxY + CURSOR_BOX_HEIGHT > allDisplaysBounds.height) {
                    cursorBoxY = mousePosition.y - CURSOR_BOX_OFFSET - CURSOR_BOX_HEIGHT;
                }

                // Compute text position
                int textX = cursorBoxX + (cursorBoxWidth - textWidth) / 2;
                int textY = cursorBoxY + (int) ((CURSOR_BOX_HEIGHT + textHeight) / 2 - ln.getDescent());


                g2d.setColor(UI.SELECTION_SIZE_BOX_COLOR);
                g2d.fillRoundRect(cursorBoxX, cursorBoxY, cursorBoxWidth, CURSOR_BOX_HEIGHT, 4, 4);

                g2d.setColor(UI.AREA_SELECTION_COLOR);
                g2d.drawString(cursorText, textX, textY);
            }

            g2d.dispose();
        }
    }

    private void addKeyboardBehaviour() {
        UI.addEscKeyShortcut(this, e -> onCancel());

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            isShiftDown = e.isShiftDown();
            return false;
        });
    }

    private void addMouseBehaviour() {
        Window window = this;
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                window.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Point mousePosition = e.getPoint();
                // See if there was a previous selection
                if (selection != null) {
                    final Rectangle actionPanelBounds = actionPanel.getBounds();
                    // There's already a selection.
                    // Hide the button bar during drag
                    setActionPanelVisible(false);

                    // Can happen if selection is so large that actionPanel is over selection
                    if (!actionPanelBounds.contains(mousePosition)) {

                        // See where the mouse press happened
                        currentOperation = getHoverOperation(mousePosition, selection);

                        //noinspection EnhancedSwitchMigration
                        switch (currentOperation) {
                            case Cursor.DEFAULT_CURSOR:
                                // We clicked outside selection, restart selection (ENHANCEMENT ? USEFUL ?)
                                resetSelection();
                                break;
                            case Cursor.MOVE_CURSOR:
                            case Cursor.NW_RESIZE_CURSOR:
                            case Cursor.N_RESIZE_CURSOR:
                            case Cursor.W_RESIZE_CURSOR:
                                // Remember offset between click position and reference (top-left corner of the selection)
                                rememberedReferenceOffset = new Point(mousePosition.x - selection.x, mousePosition.y - selection.y);
                                break;
                            case Cursor.NE_RESIZE_CURSOR:
                            case Cursor.E_RESIZE_CURSOR:
                                // Remember offset between click position and reference (top-right corner of the selection)
                                rememberedReferenceOffset = new Point(mousePosition.x - (selection.x + selection.width), mousePosition.y - selection.y);
                                break;
                            case Cursor.SW_RESIZE_CURSOR:
                            case Cursor.S_RESIZE_CURSOR:
                                // Remember offset between click position and reference (bottom-left corner of the selection)
                                rememberedReferenceOffset = new Point(mousePosition.x - selection.x, mousePosition.y - (selection.y + selection.height));
                                break;
                            case Cursor.SE_RESIZE_CURSOR:
                                // Remember offset between click position and reference (bottom-right corner of the selection)
                                rememberedReferenceOffset = new Point(mousePosition.x - (selection.x + selection.width), mousePosition.y - (selection.y + selection.height));
                                break;
                        }
                    }
                }
                if (selection == null) {
                    // Start of new selection. Remember offset between click position and reference ("opposite" corner)
                    // Creating a selection is like resizing a selection of 0,0
                    selection = new Rectangle(mousePosition.x, mousePosition.y, 0, 0);
                    currentOperation = Cursor.SW_RESIZE_CURSOR;
                    rememberedReferenceOffset = new Point(0, 0);
                    isInitialSelectionDone = false; // TODO redundant ?
                }
                window.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selection.width == 0 && selection.height == 0) {
                    // Just a click in fact
                    selection = new Rectangle(0, 0, allDisplaysBounds.width, allDisplaysBounds.height);
                    // TODO should become "hovered window", if any, when detection is implemented
                }
                currentOperation = OPERATION_NONE;
                isInitialSelectionDone = true;
                window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                setActionPanelVisible(true);
                window.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selection != null) {
                    // ENHANCEMENT: resize cursor changes when it goes over the edge
                    // Note: rememberedReferenceOffset has different meanings according to currentOperation. See mousePressed
                    final int newX = e.getX() - rememberedReferenceOffset.x;
                    final int newY = e.getY() - rememberedReferenceOffset.y;
                    int previousOperation = currentOperation;
                    switch (currentOperation) {
                        // Move selection rectangle
                        case Cursor.MOVE_CURSOR -> selection.setLocation(newX, newY);
                        // Move only one edge or one corner
                        case Cursor.W_RESIZE_CURSOR -> currentOperation = Coords.setX1(selection, newX, currentOperation);
                        case Cursor.N_RESIZE_CURSOR -> currentOperation = Coords.setY1(selection, newY, currentOperation);
                        case Cursor.NW_RESIZE_CURSOR -> {
                            currentOperation = Coords.setX1(selection, newX, currentOperation);
                            currentOperation = Coords.setY1(selection, newY, currentOperation);
                        }
                        case Cursor.E_RESIZE_CURSOR -> currentOperation = Coords.setX2(selection, newX, currentOperation);
                        case Cursor.NE_RESIZE_CURSOR -> {
                            currentOperation = Coords.setY1(selection, newY, currentOperation);
                            currentOperation = Coords.setX2(selection, newX, currentOperation);
                        }
                        case Cursor.S_RESIZE_CURSOR -> currentOperation = Coords.setY2(selection, newY, currentOperation);
                        case Cursor.SW_RESIZE_CURSOR -> {
                            currentOperation = Coords.setX1(selection, newX, currentOperation);
                            currentOperation = Coords.setY2(selection, newY, currentOperation);
                        }
                        case Cursor.SE_RESIZE_CURSOR -> {
                            currentOperation = Coords.setX2(selection, newX, currentOperation);
                            currentOperation = Coords.setY2(selection, newY, currentOperation);
                        }
                    }
                    if (previousOperation != currentOperation) {
                        updateMouseCursor();
                    }
                }
                // Paint rectangle (and cross lines if making selection)
                window.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (selection == null) {
                    // Selection not done yet
                    // Paint cross lines
                    window.repaint();
                }
                else {
                    // Selection done
                    final Rectangle actionPanelBounds = actionPanel.getBounds();
                    // Can happen if selection is so large that actionPanel is over selection
                    if (actionPanelBounds.contains(e.getPoint())) {
                        window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                    else {
                        // Determine operation that could be done by a mousePressed there, and change cursor accordingly.
                        //noinspection MagicConstant
                        window.setCursor(Cursor.getPredefinedCursor(getHoverOperation(e.getPoint(), selection)));
                    }
                }
            }


            private void updateMouseCursor() {
                if (isInitialSelectionDone) {
                    //noinspection MagicConstant
                    window.setCursor(Cursor.getPredefinedCursor(currentOperation));
                }
            }

            /**
             * Determine mouse operation based on hover point and selection.
             * Note that we return mouse cursor constants, but it is more than just a cursor:
             * the returned int value indicates which operation this area corresponds to.
             */
            private int getHoverOperation(Point point, Rectangle selection) {
                // Inside
                final Rectangle internalArea = new Rectangle(selection);
                internalArea.grow(-RESIZE_AREA_IN_MARGIN, -RESIZE_AREA_IN_MARGIN);
                if (internalArea.contains(point)) return Cursor.MOVE_CURSOR;

                // Above or Below or Left or Right
                if (point.y < selection.y - RESIZE_AREA_OUT_MARGIN
                        || point.y > selection.y + selection.height + RESIZE_AREA_OUT_MARGIN
                        || point.x < selection.x - RESIZE_AREA_OUT_MARGIN
                        || point.x > selection.x + selection.width + RESIZE_AREA_OUT_MARGIN
                ) {
                    return Cursor.DEFAULT_CURSOR;
                }

                // OK, we're in the "resize area". Determine which.
                if (point.x <= internalArea.x) {
                    // left edge
                    if (point.y <= internalArea.y) {
                        return Cursor.NW_RESIZE_CURSOR;
                    }
                    else if (point.y >= internalArea.y + internalArea.height) {
                        return Cursor.SW_RESIZE_CURSOR;
                    }
                    else {
                        return Cursor.W_RESIZE_CURSOR;
                    }
                }
                else if (point.x >= internalArea.x + internalArea.width) {
                    // right edge
                    if (point.y <= internalArea.y) {
                        return Cursor.NE_RESIZE_CURSOR;
                    }
                    else if (point.y >= internalArea.y + internalArea.height) {
                        return Cursor.SE_RESIZE_CURSOR;
                    }
                    else {
                        return Cursor.E_RESIZE_CURSOR;
                    }
                }
                else {
                    // between left and right edges (but not inside)
                    if (point.y <= internalArea.y) {
                        return Cursor.N_RESIZE_CURSOR;
                    }
                    else {
                        return Cursor.S_RESIZE_CURSOR;
                    }
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    protected int getSelectedAreaHorizontalStrokeWidth() {
        return SELECTED_AREA_STROKE_WIDTH;
    }

    @Override
    protected int getSelectedAreaVerticalStrokeWidth() {
        return SELECTED_AREA_STROKE_WIDTH;
    }

    private void setActionPanelVisible(boolean visible) {
        if (visible) {
            // Compute size to be displayed in box.
            // This is size of the selection, except if it was moved partly off screen.
            // In that case, we keep the selection unchanged (in case we want to move it back on screen),
            // but the actual capture would be cropped to the dimensions of the mega-window of course.
            // Use that size here
            final Rectangle croppedSelection = getCroppedSelection();
            captureSizeLabel.setText(croppedSelection.width + " x " + croppedSelection.height);
            boolean isValidArea = (croppedSelection.width > 5) && (croppedSelection.height > 5);
            imageButton.setEnabled(isValidArea);
            videoButton.setEnabled(isValidArea);
            positionActionPanel();
            revalidate();
        }
        actionPanel.setVisible(visible);
        revalidate();
    }

    private void resetSelection() {
        setActionPanelVisible(false);
        isInitialSelectionDone = false;
        rememberedReferenceOffset = null;
        selection = null;
        setCursor(CURSOR_NONE);
        repaint();
    }

    private Rectangle getCroppedSelection() {
        return selection.intersection(new Rectangle(0, 0, allDisplaysBounds.width, allDisplaysBounds.height));
    }

    private Capture createNewCapture(boolean isVideo) {
        Capture capture = new Capture(new SimpleDateFormat(Misc.DATETIME_FORMAT_PATTERN).format(new Date()));
        capture.setVideo(isVideo);
        return capture;
    }

    ////////////////////////////
    // EVENT HANDLERS


    private void onCaptureImage() {
        final Rectangle croppedSelection = getCroppedSelection();
        final BufferedImage capturedImg = capturedScreenImg.getSubimage(croppedSelection.x, croppedSelection.y, croppedSelection.width, croppedSelection.height);
        final Capture capture = createNewCapture(false);
        capture.setOriginalImage(capturedImg);
        final CaptureEditingFrame captureEditingFrame = new CaptureEditingFrame(starWindow, capture);
        captureEditingFrame.setVisible(true);
        dispose();
    }

    private void onCaptureVideo() {
        if (Jaffree.IS_AVAILABLE) {
            final VideoControlFrame videoControlFrame = new VideoControlFrame(starWindow, getCroppedSelection(), createNewCapture(true));
            dispose();
            videoControlFrame.setVisible(true);
        }
        else {
            UI.alertError(this, "Cannot capture video", Ginj.getAppName() + " could not find FFmpeg during startup.\nPlease re-install " + Ginj.getAppName() + " and make sure you select ffmpeg during the installation.\nIf the problem persist, please open a Github issue.");
        }
    }

    private void onRedo() {
        resetSelection();
    }

    private void onCancel() {
        dispose();
    }

}