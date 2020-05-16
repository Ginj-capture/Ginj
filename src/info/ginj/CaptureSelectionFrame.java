package info.ginj;

import info.ginj.ui.GinjButton;
import info.ginj.ui.GinjButtonBar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*

TODO :
 - Merge code of first selection with code of resize (only difference is drawing cross bar on first selection
 - Make sure resize can go past beginning (negative selection)
 - Reduce height of tooltip bar in GinjButtonBar
 - Align tooltip above its button

Note: an undecorated JFrame is required instead of a JWindow, otherwise keyboard events (ESC) are not captured
*/

public class CaptureSelectionFrame extends JFrame {

    public static final Color COLOR_ORANGE = new Color(251, 168, 25);
    public static final Color COLOR_DIMMMED_SCREEN = new Color(0, 0, 0, 51);
    public static final Color COLOR_SIZE_BOX = new Color(0, 0, 0, 128);

    public static final int SIZE_BOX_WIDTH = 75;
    public static final int SIZE_BOX_HEIGHT = 18;
    public static final int SIZE_BOX_OFFSET = 8;

    private static final int RESIZE_AREA_IN_MARGIN = 5;
    private static final int RESIZE_AREA_OUT_MARGIN = 10;

    private static final int OPERATION_NONE = -1;

    // Caching
    private final Dimension screenSize;
    // See https://stackoverflow.com/a/10687248
    private final Cursor CURSOR_NONE = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), null);


    // Current state
    private Point rememberedStartPoint = null; // filled when selecting or dragging
    private Rectangle selection; // filled when selection is done
    private int currentOperation = OPERATION_NONE;

    private final JPanel actionPanel;
    private JLabel sizeLabel;

    public CaptureSelectionFrame() {
        super();
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//screenSize.setSize(screenSize.width/2, screenSize.height);

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        JComponent contentPane = new CaptureMainPane();
        setContentPane(contentPane);
        addMouseBehaviour();

        setLayout(null); // Allow absolute positioning of button bar

        // Prepare button bar
        actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        JPanel buttonBar = new GinjButtonBar();
        try {
            final JButton imageButton = new GinjButton("Capture image", new ImageIcon(ImageIO.read(getClass().getResource("img/b_image.png"))));
            imageButton.addActionListener(e -> onCaptureImage());
            buttonBar.add(imageButton);
            final JButton videoButton = new GinjButton("Capture video", new ImageIcon(ImageIO.read(getClass().getResource("img/b_video.png"))));
            videoButton.addActionListener(e -> onCaptureVideo());
            buttonBar.add(videoButton);
            final JButton redoButton = new GinjButton("Redo selection", new ImageIcon(ImageIO.read(getClass().getResource("img/b_redo.png"))));
            redoButton.addActionListener(e -> onRedo());
            buttonBar.add(redoButton);
            final JButton cancelButton = new GinjButton("Cancel",new ImageIcon(ImageIO.read(getClass().getResource("img/b_cancel.png"))));
            cancelButton.addActionListener(e -> onCancel());
            buttonBar.add( cancelButton);
            sizeLabel = new JLabel("10000 x 10000");
            buttonBar.add(sizeLabel);
        }
        catch (IOException e) {
            System.out.println("Error loading capture button images");
            e.printStackTrace();
            System.exit(Ginj.ERR_STATUS_LOAD_IMG);
        }
        actionPanel.add(buttonBar);

        actionPanel.setSize(computeSize(actionPanel));

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

    /**
     * This method computes the size of the given panel by adding it in a temporary window
     * Warning, must be called before adding the panel to its final parent, because it will be removed from it otherwise
     */
    private Dimension computeSize(JPanel actionPanel) {
        JWindow window = new JWindow();
        window.setLayout(new BorderLayout());
        window.getContentPane().add(actionPanel);
        window.pack();
        return window.getSize();
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
        private BufferedImage capturedScreenImg;
        private Polygon screenShape;
        private Font font;
        private FontRenderContext fontRenderContext;

        public CaptureMainPane() {
            try {
                Robot robot = new Robot();
                Rectangle rectangle = new Rectangle(screenSize);
                capturedScreenImg = robot.createScreenCapture(rectangle);

                // Prepare a shape for the full screen
                screenShape = new Polygon();
                screenShape.addPoint(0, 0);
                screenShape.addPoint(screenSize.width, 0);
                screenShape.addPoint(screenSize.width, screenSize.height);
                screenShape.addPoint(0, screenSize.height);
            }
            catch (AWTException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return screenSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.drawImage(capturedScreenImg, 0, 0, this);
            Point mousePosition = getMousePosition();
            Rectangle rectangleToDraw = null;

            // Determine rectangle to draw (if any)
            if (selection != null) {
                // Selection done
                rectangleToDraw = selection;
            }
            else if (rememberedStartPoint != null) {
                // Selection in progress
                rectangleToDraw = getSelectionRectangle(rememberedStartPoint, mousePosition);
            }

            if (rectangleToDraw != null) {
                // Dim the rest of the screen
                Area area = new Area(screenShape);
                area.subtract(new Area(rectangleToDraw));
                g2d.setColor(COLOR_DIMMMED_SCREEN);
                g2d.fill(area);

                // Draw the selection rectangle
                g2d.setColor(COLOR_ORANGE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(rectangleToDraw.x, rectangleToDraw.y, rectangleToDraw.width, rectangleToDraw.height);
            }

            if (selection == null && mousePosition != null) {
                // Selection in progress

                // Draw cross lines
                g2d.setColor(COLOR_ORANGE);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(mousePosition.x, 0, mousePosition.x, (int) screenSize.getHeight());
                g2d.drawLine(0, mousePosition.y, (int) screenSize.getWidth(), mousePosition.y);
            }

            // Determine size to print in size box
            String sizeText = null;
            if ((selection == null && mousePosition != null)) {
                if (rectangleToDraw == null) {
                    // No (partial) selection yet show screen size
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

            if (sizeText != null) {
                // Draw the selection size box
                g2d.setColor(COLOR_SIZE_BOX);
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
                g2d.setColor(COLOR_ORANGE);
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

    /**
     * Creates a rectangle given the two opposite points.
     * Note that is doesn't matter if start is right/left or above/below end.
     */
    private static Rectangle getSelectionRectangle(Point startPoint, Point endPoint) {
        return new Rectangle(
                Math.min(startPoint.x, endPoint.x),
                Math.min(startPoint.y, endPoint.y),
                Math.abs(startPoint.x - endPoint.x),
                Math.abs(startPoint.y - endPoint.y)
        );
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
                // If there was a previous selection
                Point mousePosition = e.getPoint();
                if (selection != null) {
                    // There's already a selection.
                    // Hide the button bar during drag
                    setActionPanelVisible(false);

                    // See where the mouse press happened
                    currentOperation = getHoverOperation(mousePosition, selection);

                    //noinspection EnhancedSwitchMigration
                    switch (currentOperation) {
                        case Cursor.DEFAULT_CURSOR:
                            // We clicked outside, restart selection (ENHANCEMENT)
                            resetSelection();
                            break;
                        case Cursor.MOVE_CURSOR:
                        case Cursor.NW_RESIZE_CURSOR:
                        case Cursor.N_RESIZE_CURSOR:
                        case Cursor.W_RESIZE_CURSOR:
                            // Remember offset between click position and top-left corner of the selection
                            rememberedStartPoint = new Point(mousePosition.x - selection.x, mousePosition.y - selection.y);
                            break;
                        case Cursor.NE_RESIZE_CURSOR:
                        case Cursor.E_RESIZE_CURSOR:
                            // Remember offset between click position and top-right corner of the selection
                            rememberedStartPoint = new Point(mousePosition.x - (selection.x + selection.width), mousePosition.y - selection.y);
                            break;
                        case Cursor.SW_RESIZE_CURSOR:
                        case Cursor.S_RESIZE_CURSOR:
                            // Remember offset between click position and bottom-left corner of the selection
                            rememberedStartPoint = new Point(mousePosition.x - selection.x, mousePosition.y - (selection.y + selection.height));
                            break;
                        case Cursor.SE_RESIZE_CURSOR:
                            // Remember offset between click position and bottom-right corner of the selection
                            rememberedStartPoint = new Point(mousePosition.x - (selection.x + selection.width), mousePosition.y - (selection.y + selection.height));
                            break;
                    }
                }

                if (rememberedStartPoint == null) {
                    // Start of "drag". Remember mouse position
                    rememberedStartPoint = mousePosition;
                }
                window.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selection == null) {
                    // End of dragged rectangle selection: store end position
                    selection = getSelectionRectangle(rememberedStartPoint, e.getPoint());
                    window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                currentOperation = OPERATION_NONE;
                setActionPanelVisible(true);
                window.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selection != null) {
                    // Note: rememberedStartPoint has different meanings according to currentOperation. See mousePressed
                    final int newX = e.getX() - rememberedStartPoint.x;
                    final int newY = e.getY() - rememberedStartPoint.y;
                    switch (currentOperation) {
                        // Move selection rectangle
                        case Cursor.MOVE_CURSOR -> selection.setLocation(newX, newY);
                        // Move only one edge or one corner
                        case Cursor.W_RESIZE_CURSOR -> setX1(selection, newX);
                        case Cursor.N_RESIZE_CURSOR -> setY1(selection, newY);
                        case Cursor.NW_RESIZE_CURSOR -> {
                            setX1(selection, newX);
                            setY1(selection, newY);
                        }
                        case Cursor.E_RESIZE_CURSOR -> setX2(selection, newX);
                        case Cursor.NE_RESIZE_CURSOR -> {
                            setY1(selection, newY);
                            setX2(selection, newX);
                        }
                        case Cursor.S_RESIZE_CURSOR -> setY2(selection, newY);
                        case Cursor.SW_RESIZE_CURSOR -> {
                            setX1(selection, newX);
                            setY2(selection, newY);
                        }
                        case Cursor.SE_RESIZE_CURSOR -> {
                            setX2(selection, newX);
                            setY2(selection, newY);
                        }
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
                    // Determine operation that could be done by a mousePressed there, and change cursor accordingly.
                    //noinspection MagicConstant
                    window.setCursor(Cursor.getPredefinedCursor(getHoverOperation(e.getPoint(), selection)));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Single click means full window
                // TODO should become hovered window when detection is implemented
                selection = new Rectangle(screenSize);
                setActionPanelVisible(true);
            }

            ////////////////////////////////
            // Coordinate utils

            // Only move left edge of rectangle, keeping other edges the same
            private void setX1(Rectangle rectangle, int newX1) {
                int oldX1 = rectangle.x;
                rectangle.setLocation(newX1, rectangle.y);
                rectangle.setSize(rectangle.width + oldX1 - newX1, rectangle.height);
            }

            // Only move top edge of rectangle, keeping other edges the same
            private void setY1(Rectangle rectangle, int newY1) {
                int oldY1 = rectangle.y;
                rectangle.setLocation(rectangle.x, newY1);
                rectangle.setSize(rectangle.width, rectangle.height + oldY1 - newY1);
            }

            // Only move right edge of rectangle, keeping other edges the same
            private void setX2(Rectangle rectangle, int newX2) {
                int oldX2 = rectangle.x + rectangle.width;
                rectangle.setSize(rectangle.width - oldX2 + newX2, rectangle.height);
            }

            // Only move bottom edge of rectangle, keeping other edges the same
            private void setY2(Rectangle rectangle, int newY2) {
                int oldY2 = rectangle.y + rectangle.height;
                rectangle.setSize(rectangle.width, rectangle.height - oldY2 + newY2);
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
            sizeLabel.setText(selection.width + " x " + selection.height);
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
        Point[] candidatePositions = new Point[] {
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
        rememberedStartPoint = null;
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
        // Crop now to screen dimensions (check that selection can be moved in and out of screen before this point)
        // TODO
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