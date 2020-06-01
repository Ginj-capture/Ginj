package info.ginj;

import info.ginj.action.*;
import info.ginj.tool.Overlay;
import info.ginj.ui.DragInsensitiveMouseClickListener;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ImageEditorPane extends JLayeredPane {
    private final CaptureEditingFrame frame;
    private final BufferedImage capturedImg;
    private final Dimension capturedImgSize;

    private Overlay selectedOverlay;

    public ImageEditorPane(CaptureEditingFrame frame, BufferedImage capturedImg) {
        super();
        this.frame = frame;
        this.capturedImg = capturedImg;
        capturedImgSize = new Dimension(capturedImg.getWidth(), capturedImg.getHeight());
        addMouseEditingBehaviour();
        addKeyboardShortcuts(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(capturedImg, 0, 0, this);
    }

    @Override
    public Dimension getPreferredSize() {
        return capturedImgSize;
    }

    @Override
    public Dimension getMaximumSize() {
        return capturedImgSize;
    }

    private void addMouseEditingBehaviour() {
        final ImageEditorPane imagePanel = this;
        MouseInputListener mouseListener = new DragInsensitiveMouseClickListener(new MouseInputAdapter() {
            private int selectedHandleIndex;
            Point clicked;
            AbstractUndoableAction currentAction = null;

            public void mousePressed(MouseEvent e) {
                clicked = e.getPoint();

                // Find clicked component
                Overlay foundOverlay = null;
                // Iterate in reverse direction to check closest first
                // Note: JLayeredPane guarantees components are returned based on their layer order. See implementation of JLayeredPane.highestLayer()
                for (Component component : imagePanel.getComponents()) {
                    if (component instanceof Overlay) {
                        Overlay overlay = (Overlay) component;
                        if (overlay.containsPoint(clicked)) {
                            foundOverlay = overlay;
                            break;
                        }
                    }
                    else {
                        System.err.println("Encountered unexpected component: " + component);
                    }
                }
                selectedOverlay = foundOverlay; // Note: we only update it here

                if (selectedOverlay != null) {
                    // OK, we're in a component.
                    // See if it's in a handle

                    selectedOverlay.setEditInProgress(true);

                    selectedHandleIndex = selectedOverlay.getHandleIndexAt(clicked);
                    if (selectedHandleIndex == Overlay.NO_INDEX) {
                        // Initate a move
                        currentAction = new MoveOverlayAction(selectedOverlay, e.getPoint());
                    }
                    else {
                        // Initiate a resize
                        currentAction = new ResizeOverlayAction(selectedOverlay, selectedHandleIndex, selectedOverlay.getHandles()[selectedHandleIndex]);
                    }
                }
                else {
                    // Out of all components.
                    // Create a new one
                    final Overlay overlay = frame.currentTool.createComponent(clicked, frame.getCurrentColor());
                    overlay.setBounds(0, 0, capturedImgSize.width, capturedImgSize.height);
                    currentAction = new AddOverlayAction(overlay, imagePanel);
                    currentAction.execute();
                    selectedHandleIndex = 0;
                }
                repaint();
            }

            public void mouseDragged(MouseEvent e) {
                final Point mousePosition = e.getPoint();
                if (selectedHandleIndex == Overlay.NO_INDEX) {
                    // Whole component is dragged
                    // During drag, we move the whole component (which is a canvas of the same size as the image) to follow the mouse
                    selectedOverlay.setLocation(mousePosition.x - clicked.x, mousePosition.y - clicked.y);
                }
                else {
                    // Only a handle is dragged
                    selectedOverlay.moveHandle(selectedHandleIndex, mousePosition);
                }
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                if (currentAction == null) {
                    System.err.println("Mouse released with no currentAction !");
                }
                else {
                    final Point released = e.getPoint();
                    if (!hasMouseMoved(clicked, released)) {
                        if (currentAction instanceof AddOverlayAction) {
                            // False operation
                            imagePanel.remove(selectedOverlay);
                        }
                    }
                    else {
                        currentAction.setTargetPoint(released);
                        if (currentAction instanceof MoveOverlayAction) {
                            // Upon release, we reset the canvas position to 0,0 and execute the action that moves the drawing itself on the canvas
                            selectedOverlay.setLocation(0, 0);
                            currentAction.execute();
                        }
                        frame.addUndoableAction(currentAction);
                        currentAction = null;
                    }
                    selectedOverlay.setEditInProgress(false);
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e){
                if(e.getClickCount()==2) {
                    if (selectedOverlay != null) {
                        // ENHANCEMENT
                        currentAction = new BringOverlayToFrontAction(selectedOverlay, imagePanel);
                        currentAction.execute();
                        frame.addUndoableAction(currentAction);
                    }
                }
            }

            private boolean hasMouseMoved(Point clicked, Point released) {
                return Math.abs(clicked.x - released.x) > 5 || Math.abs(clicked.y - released.y) > 5;
            }
        });
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }


    private void addKeyboardShortcuts(ImageEditorPane imageEditorPane) {

        // Capture keyboard events of the whole window
        final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(undoKey, "undo");
        getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.attemptUndo();
            }
        });

        KeyStroke redoKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        KeyStroke redoKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(redoKey1, "redo");
        inputMap.put(redoKey2, "redo");
        getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.attemptRedo();
            }
        });

        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        inputMap.put(deleteKey, "delete");
        getActionMap().put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedOverlay != null) {
                    final DeleteOverlayAction deleteOverlayAction = new DeleteOverlayAction(selectedOverlay, imageEditorPane);
                    deleteOverlayAction.execute();
                    frame.addUndoableAction(deleteOverlayAction);
                    // Reselect previous one -- ENHANCEMENT
                    if (getComponentCount() > 0) {
                        final Component component = getComponent(0);
                        if (component instanceof Overlay) {
                            setSelectedOverlay((Overlay) component);
                        }
                    }
                    repaint();
                }
            }
        });

        setFocusable(true);
        requestFocusInWindow();
    }


    /**
     * Sets the given overlay as "selected".
     * @param overlay the overlay to select, or deselect all if null
     */
    public void setSelectedOverlay(Overlay overlay) {
        if (selectedOverlay != overlay) {
            // De-select previous one
            if (selectedOverlay != null) {
                selectedOverlay.setSelected(false);
            }
            if (overlay != null) {
                overlay.setSelected(true);
                frame.setCurrentColor(overlay.getColor());
                frame.updateColorButtonIcon();
            }
            selectedOverlay = overlay;
        }
        repaint();
    }

    public void setColorOfSelectedOverlay(Color color) {
        if (selectedOverlay != null) {
            final ChangeOverlayColorAction action = new ChangeOverlayColorAction(selectedOverlay, color);
            frame.addUndoableAction(action);
            action.execute();
            repaint();
        }
    }
}
