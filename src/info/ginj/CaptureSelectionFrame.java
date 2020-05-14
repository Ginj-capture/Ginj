package info.ginj;

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
import java.util.HashMap;
import java.util.Map;

/*

TODO :
 - React to resize (on border) or move (on selection)
 - Hide mouse cursor during the selection phase
 - Show mouse cursor once selection is done
 - Show resize cursor if hovering on selection border, or cross-arrow if hovering inside selection
 - Show button bar

Note: an undecorated JFrame is required instead of a JWindow, otherwise keyboard events (ESC) are not captured
*/

public class CaptureSelectionFrame extends JFrame {

    public static final Color COLOR_ORANGE = new Color(251, 168, 25);
    public static final Color COLOR_DIMMMED_SCREEN = new Color(0, 0, 0, 51);
    public static final Color COLOR_SIZE_BOX = new Color(0, 0, 0, 128);

    public static final int SIZE_BOX_WIDTH = 75;
    public static final int SIZE_BOX_HEIGHT = 18;
    public static final int SIZE_BOX_OFFSET = 8;

    // Caching
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    // Current state
    private Point selectionStartPoint = null;
    private Point selectionEndPoint = null;

    public CaptureSelectionFrame() {
        super();
        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        JComponent contentPane = new CaptureMainPane();
        setContentPane(contentPane);
        addMouseBehaviour();

        addKeyboardShortcuts();

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
        private Polygon screenShape;

        private BufferedImage capturedScreenImg;
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
            Rectangle selection = null;

            // Draw rectangle
            if (selectionStartPoint != null) {
                // Determine the end of the rectangle
                Point endPoint = selectionEndPoint;
                if (endPoint == null) {
                    // Selection not finished yet
                    endPoint = mousePosition;
                }
                // Create the corresponding rectangle
                selection = new Rectangle(
                        Math.min(selectionStartPoint.x, endPoint.x),
                        Math.min(selectionStartPoint.y, endPoint.y),
                        Math.abs(selectionStartPoint.x - endPoint.x),
                        Math.abs(selectionStartPoint.y - endPoint.y)
                );

                // Dim the rest of the screen
                Area area = new Area(screenShape);
                area.subtract(new Area(selection));
                g2d.setColor(COLOR_DIMMMED_SCREEN);
                g2d.fill(area);

                // Draw the selection rectangle
                g2d.setColor(COLOR_ORANGE);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(selection.x, selection.y, selection.width, selection.height);
            }

            if (selectionEndPoint == null && mousePosition != null) {
                // Draw cross lines
                g2d.setColor(COLOR_ORANGE);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(mousePosition.x, 0, mousePosition.x, (int) screenSize.getHeight());
                g2d.drawLine(0, mousePosition.y, (int) screenSize.getWidth(), mousePosition.y);

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
                // And size in pixels
                String sizeText;
                if (selection == null) {
                    // No (partial) selection yet show screen size
                    // Should become the candidate window we're hovering on
                    sizeText = screenSize.width + " x " + screenSize.height;
                }
                else {
                    // We're dragging, show current size
                    sizeText = selection.width + " x " + selection.height;
                }
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
                int x1 = sizeBoxX + (SIZE_BOX_WIDTH - textWidth)/2;
                int y1 = sizeBoxY + (int)((SIZE_BOX_HEIGHT + textHeight)/2 - ln.getDescent());
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
                // Start of rectangle selection: store start position
                selectionStartPoint = e.getPoint();
                selectionEndPoint = null;
                window.repaint();
            }

            // Move window to border closest to center
            @Override
            public void mouseReleased(MouseEvent e) {
                // End of dragged rectangle selection: store end position
                selectionEndPoint = e.getPoint();
                window.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Paint cross lines and rectangle
                window.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Paint cross lines
                window.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Single click means full window
                selectionStartPoint = new Point(0, 0);
                selectionEndPoint = new Point((int)screenSize.getWidth(), (int) screenSize.getHeight());
            }

        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void positionWindowOnStartup() {
        setPreferredSize(screenSize);
        setLocation(0, 0);
//        computeButtonPositions(retrievedX, retrievedY);
    }

    ////////////////////////////
    // EVENT HANDLERS


    private void onCaptureImage() {
        // TODO
    }

    private void onCaptureVideo() {
        // TODO
    }

    private void onRedo() {
        selectionStartPoint = null;
        selectionEndPoint = null;
    }

    private void onCancel() {
        dispose();
    }

}