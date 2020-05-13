package info.ginj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * UI transparency based on sample code by MadProgrammer at https://stackoverflow.com/questions/26205164/java-custom-shaped-frame-using-image
 */
/*
TODO : when window loses the focus, it cannot get it again and close with ESC key
TODO : it seems the onclick handler does not work (add breakpoint ?)
*/

public class CaptureSelectionWindow {

    // Caching
    private final Dimension screenSize;

    // Current state
    private boolean isSelecting = false;
    private Point startPoint = null;

    JFrame frame = new JFrame();

    public CaptureSelectionWindow() {

        screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setUndecorated(true);
//        frame.pack();
//        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        addKeyboardShortcuts(frame);

        JWindow window = new JWindow(frame);
        window.setVisible(true);
        window.setBounds(200,200,500,500);
        window.setBackground(new Color(0, 0, 0, 0));

        CaptureMainPane contentPane = new CaptureMainPane();
        window.setContentPane(contentPane);

//        positionWindowOnStartup();
        addMouseBehaviour(contentPane);
        window.setAlwaysOnTop(true);
    }

    private void addKeyboardShortcuts(JFrame frame) {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                }
            }
        });
        frame.setFocusableWindowState(true);
        frame.setFocusable(true);
        frame.setFocusTraversalKeysEnabled(true);
    }


    public class CaptureMainPane extends JPanel {

        private BufferedImage capturedScreen;

        public CaptureMainPane() {
            try {
                Robot robot = new Robot();
                Rectangle rectangle = new Rectangle(screenSize);
                capturedScreen = robot.createScreenCapture(rectangle);
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
            g2d.drawImage(capturedScreen, 0, 0, this);
            // draw cross lines
            if (startPoint != null) {
                g2d.setColor(new Color(255, 0, 0));
                g2d.drawLine(startPoint.x, 0, startPoint.x, (int) screenSize.getHeight());
                g2d.drawLine(0, startPoint.y, (int) screenSize.getWidth(), startPoint.y);
            }
            g2d.dispose();
        }
    }

    private void addMouseBehaviour(CaptureMainPane contentPane) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private int pX;
            private int pY;

            @Override
            public void mousePressed(MouseEvent e) {
                // Start of rectangle selection
                // Get x,y and store them
                pX = e.getX();
                pY = e.getY();
                isSelecting = true;
                frame.repaint();
            }

            // Move window to border closest to center
            @Override
            public void mouseReleased(MouseEvent e) {
                // End of rectangle selection
                isSelecting = false;
                frame.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Draw the rectangle
                frame.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Show crossbar
                frame.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                frame.setAlwaysOnTop(false);
                frame.setAlwaysOnTop(true);

                // Single click means full window ()
                /*
                if (!isSelecting) {
                    int clickedButtonId = getButtonIdAtLocation(e.getX(), e.getY());
                    // ignore other clicks
                    switch (clickedButtonId) {
                        case BTN_CAPTURE -> onCapture();
                        case BTN_HISTORY -> onHistory();
                        case BTN_MORE -> onMore();
                    }
                    isWindowFocused = false;
                    frame.repaint();
                }
                 */
            }

        };

        frame.addMouseListener(mouseAdapter);
        frame.addMouseMotionListener(mouseAdapter);
    }

    private void positionWindowOnStartup() {

        frame.setLocation(200,200);

//        computeButtonPositions(retrievedX, retrievedY);
    }

    ////////////////////////////
    // EVENT HANDLERS


    private void onHistory() {
        JOptionPane.showMessageDialog(null, "This should open the history window");
    }


    private void onMore() {
        JOptionPane.showMessageDialog(null, "This should open the more window - Now exiting...");
        System.exit(0);
    }
}