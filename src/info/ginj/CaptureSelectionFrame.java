package info.ginj;

import info.ginj.ui.GinjLabel;
import info.ginj.ui.GinjLowerButton;
import info.ginj.ui.GinjLowerButtonBar;
import info.ginj.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the full screen window on which the area selection is made.
 * It takes up the full screen, captures the screen on startup and uses it as background, then paints the selection box
 * above it.
 * Note: an undecorated JFrame is required instead of a JWindow, otherwise keyboard events (ESC) are not captured
 */
public class CaptureSelectionFrame extends JFrame {

    public static final int SIZE_BOX_WIDTH = 75;
    public static final int SIZE_BOX_HEIGHT = 18;
    public static final int SIZE_BOX_OFFSET = 8;

    private static final int RESIZE_AREA_IN_MARGIN = 5;
    private static final int RESIZE_AREA_OUT_MARGIN = 10;

    private static final int OPERATION_NONE = -1;

    // Caching
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    // See https://stackoverflow.com/a/10687248
    private final Cursor CURSOR_NONE = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), null);


    // Current state
    private BufferedImage capturedScreenImg;
    private Point rememberedReferenceOffset = null; // filled when selecting or dragging
    private Rectangle selection; // filled when selection is done
    private int currentOperation = OPERATION_NONE;
    private boolean isInitialSelectionDone;

    private final JPanel actionPanel;
    private final GinjLabel captureSizeLabel;
    private final JButton imageButton;
    private final JButton videoButton;

    public CaptureSelectionFrame() {
        super();
// Simulate a half screen to be able to debug in parallel of "full screen" capture window on top
//screenSize.setSize(screenSize.width/2, screenSize.height);

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        JComponent contentPane = new CaptureMainPane();
        setContentPane(contentPane);
        addMouseBehaviour();

        setLayout(null); // Allow absolute positioning of button bar

        // Prepare button bar
        actionPanel = new JPanel(); // To add a margin around buttonBar
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actionPanel.setName("GinjPanel"); // To be used as a selector in laf.xml
        JPanel buttonBar = new GinjLowerButtonBar();

        imageButton = new GinjLowerButton("Capture image", Util.createIcon(getClass().getResource("img/icon/image.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        imageButton.addActionListener(e -> onCaptureImage());
        buttonBar.add(imageButton);
        videoButton = new GinjLowerButton("Capture video", Util.createIcon(getClass().getResource("img/icon/video.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        videoButton.addActionListener(e -> onCaptureVideo());
        buttonBar.add(videoButton);
        final JButton redoButton = new GinjLowerButton("Redo selection", Util.createIcon(getClass().getResource("img/icon/redo_selection.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        redoButton.addActionListener(e -> onRedo());
        buttonBar.add(redoButton);
        final JButton cancelButton = new GinjLowerButton("Cancel", Util.createIcon(getClass().getResource("img/icon/cancel.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        cancelButton.addActionListener(e -> onCancel());
        buttonBar.add(cancelButton);
        captureSizeLabel = new GinjLabel("9999 x 9999");
        buttonBar.add(captureSizeLabel);

        actionPanel.add(buttonBar);
        Util.packPanel(actionPanel);
        contentPane.add(actionPanel);

        addKeyboardShortcuts();

        resetSelection();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        requestFocus();

        positionWindowOnStartup();
        setAlwaysOnTop(true);
    }

    private void addKeyboardShortcuts() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    onCancel();
                }
            }
        });
    }


    public class CaptureMainPane extends JPanel {
        // Caching
        private final Image dimmedScreenImg;
        private Font font;
        private FontRenderContext fontRenderContext;

        public CaptureMainPane() {
            try {
                Robot robot = new Robot();
                Rectangle rectangle = new Rectangle(screenSize);
                capturedScreenImg = robot.createScreenCapture(rectangle);
            }
            catch (AWTException e) {
                e.printStackTrace();
            }

            // Prepared a dimmed & greyscale version to be used for "unselected area"
            dimmedScreenImg = Util.makeDimmedImage(capturedScreenImg);
        }

        @Override
        public Dimension getPreferredSize() {
            return screenSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            Point mousePosition = getMousePosition();
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
                g2d.setClip(0, 0, screenSize.width, screenSize.height);

                // Draw the selection rectangle
                g2d.setColor(Util.AREA_SELECTION_COLOR);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(rectangleToDraw.x, rectangleToDraw.y, rectangleToDraw.width, rectangleToDraw.height);
            }
            else {
                // Draw the non-dimmed image on the whole screen
                g2d.drawImage(capturedScreenImg, 0, 0, this);
            }

            if (!isInitialSelectionDone && mousePosition != null) {
                // First selection in progress

                // Draw cross lines
                g2d.setColor(Util.AREA_SELECTION_COLOR);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(mousePosition.x, 0, mousePosition.x, (int) screenSize.getHeight());
                g2d.drawLine(0, mousePosition.y, (int) screenSize.getWidth(), mousePosition.y);
            }

            // Determine size to print in size box
            String sizeText = null;
            if (selection == null) {
                if (rectangleToDraw == null) {
                    // No (partial) selection yet, show screen size
                    // TODO : "screen" to be replaced by "hovered window" when window detection is implemented
                    sizeText = screenSize.width + " x " + screenSize.height;
                }
                else {
                    // We're dragging, show current size
                    sizeText = rectangleToDraw.width + " x " + rectangleToDraw.height;
                }
            }
            else if (currentOperation != OPERATION_NONE && currentOperation != Cursor.DEFAULT_CURSOR && currentOperation != Cursor.MOVE_CURSOR) {
                sizeText = selection.width + " x " + selection.height;
            }

            if (sizeText != null && mousePosition != null) {
                // Use antialiasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw the selection size box
                g2d.setColor(Util.SELECTION_SIZE_BOX_COLOR);
                int sizeBoxX = mousePosition.x + SIZE_BOX_OFFSET;
                if (sizeBoxX + SIZE_BOX_WIDTH > screenSize.width) {
                    sizeBoxX = mousePosition.x - SIZE_BOX_OFFSET - SIZE_BOX_WIDTH;
                }
                int sizeBoxY = mousePosition.y + SIZE_BOX_OFFSET;
                if (sizeBoxY + SIZE_BOX_HEIGHT > screenSize.height) {
                    sizeBoxY = mousePosition.y - SIZE_BOX_OFFSET - SIZE_BOX_HEIGHT;
                }
                g2d.fillRoundRect(sizeBoxX, sizeBoxY, SIZE_BOX_WIDTH, SIZE_BOX_HEIGHT, 4, 4);

                // And print size
                g2d.setColor(Util.AREA_SELECTION_COLOR);
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
                int textWidth = (int) font.getStringBounds(sizeText, fontRenderContext).getWidth();
                LineMetrics ln = font.getLineMetrics(sizeText, fontRenderContext);
                int textHeight = (int) (ln.getAscent() + ln.getDescent());
                int x1 = sizeBoxX + (SIZE_BOX_WIDTH - textWidth) / 2;
                int y1 = sizeBoxY + (int) ((SIZE_BOX_HEIGHT + textHeight) / 2 - ln.getDescent());
                g2d.drawString(sizeText, x1, y1);
            }

            g2d.dispose();
        }

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
                    selection = new Rectangle(screenSize);
                    // TODO should become hovered window, if any, when detection is implemented
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

    private void setActionPanelVisible(boolean visible) {
        if (visible) {
            // Compute size to be displayed in box.
            // This is size of the selection, except if it was moved partly off screen.
            // In that case, we keep the selection unchanged (in case we want to move it back on screen),
            // but the actual capture would be cropped to the screen dimensions of course.
            // Use that size here
            final Rectangle croppedSelection = selection.intersection(new Rectangle(screenSize));
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

    private void positionActionPanel() {
        final int barWidth = actionPanel.getWidth();
        final int barHeight = actionPanel.getHeight();

        // Find the best position according to selection and bar size
        // Note: not the exact same strategy as the original, but close...
        Rectangle bestBounds = null;
        Point[] candidatePositions = new Point[]{
                new Point(selection.x, selection.y + selection.height), // Below bottom left
                new Point(selection.x, selection.y - barHeight), // Above top left
                new Point(selection.x - barWidth, selection.y), // Next to top left
                new Point(selection.x + selection.width, selection.y + selection.height - barHeight), // Next to bottom right
                new Point(0, selection.y + selection.height), // Below, on left screen edge
                new Point(0, selection.y - barHeight), // Above, on left screen edge
                new Point(selection.x, selection.y + selection.height - barHeight), // Over, at bottom left of selection
                new Point(selection.x, selection.y), // Over, at top left of selection
                new Point(0, screenSize.height - barHeight) // Over, at bottom left of screen
        };
        Rectangle screenRectangle = new Rectangle(screenSize);
        for (Point candidatePosition : candidatePositions) {
            final Rectangle candidateBounds = new Rectangle(candidatePosition.x, candidatePosition.y, barWidth, barHeight);
            if (screenRectangle.contains(candidateBounds)) {
                bestBounds = candidateBounds;
                break;
            }
        }
        if (bestBounds == null) {
            Point p = candidatePositions[candidatePositions.length - 1];
            bestBounds = new Rectangle(p.x, p.y, barWidth, barHeight);
        }
        actionPanel.setBounds(bestBounds);
    }

    private void resetSelection() {
        setActionPanelVisible(false);
        isInitialSelectionDone = false;
        rememberedReferenceOffset = null;
        selection = null;
        setCursor(CURSOR_NONE);
        repaint();
    }

    private void positionWindowOnStartup() {
        setPreferredSize(screenSize);
        setLocation(0, 0);
    }

    ////////////////////////////
    // EVENT HANDLERS


    private void onCaptureImage() {
        // Crop image
        final Rectangle croppedSelection = selection.intersection(new Rectangle(screenSize));
        final BufferedImage capturedImg = capturedScreenImg.getSubimage(croppedSelection.x, croppedSelection.y, croppedSelection.width, croppedSelection.height);
        final CaptureEditingFrame captureEditingFrame = new CaptureEditingFrame(capturedImg);
        captureEditingFrame.setVisible(true);
        dispose();
    }

    private void onCaptureVideo() {
        // TODO
    }

    private void onRedo() {
        resetSelection();
    }

    private void onCancel() {
        dispose();
    }

}