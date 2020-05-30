package info.ginj;

import info.ginj.action.AbstractUndoableAction;
import info.ginj.action.AddOverlayAction;
import info.ginj.tool.Overlay;
import info.ginj.ui.DragInsensitiveMouseClickListener;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.UndoableEditEvent;
import java.awt.*;
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
            Point clicked;
            AbstractUndoableAction currentAction = null;

            public void mousePressed(MouseEvent e) {
                clicked = e.getPoint();

                // Find selected component
                setSelectedOverlay(null);
                // Iterate in reverse direction to check closest first
                // Note: JLayeredPane guarantees components are returned based on their layer order. See implementation of JLayeredPane.highestLayer()
                for (Component component : imagePanel.getComponents()) {
                    if (component instanceof Overlay) {
                        Overlay overlay = (Overlay) component;
                        if (overlay.isInComponent(clicked)) {
                            setSelectedOverlay(overlay);
                            break;
                        }
                    }
                    else {
                        System.err.println("Encountered unexpected component: " + component);
                    }
                }
                if (selectedOverlay != null) {
                    // OK, we're in a component.
                    // See if it's in a handle
/*
                    int selectedHandle = selectedOverlay.getHandleNumberAt(clicked);
                    if (selectedHandle == -1) {
                        // Initate a move
                    }
                    else {
                        // Initiate a resize
                        // Add creation to undo stack
                        ModifyOverlayAction action = new ModifyOverlayAction(selectedOverlay, imagePanel);
                        action.execute();
                        frame.undoManager.undoableEditHappened(new UndoableEditEvent(imagePanel, action));
                    }
*/
                }
                else {
                    // Out of all components.
                    // Create a new one
                    final Overlay overlay = frame.currentTool.createComponent(clicked, frame.currentColor);
                    overlay.setBounds(0, 0, capturedImgSize.width, capturedImgSize.height);
                    currentAction = new AddOverlayAction(overlay, imagePanel);
                    currentAction.execute();
                }
                // TODO Remember the "before" state to be able to undo
                repaint();
            }

            public void mouseDragged(MouseEvent e) {
                selectedOverlay.moveHandle(0, e.getPoint());
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                if (selectedOverlay.hasNoSize()) {
                    // False operation
                    imagePanel.remove(selectedOverlay);
                    repaint();
                }
                else {
                    if (currentAction != null) {
                        System.out.println("Adding Action: " + currentAction.getPresentationName());
                        frame.undoManager.undoableEditHappened(new UndoableEditEvent(imagePanel, currentAction));
                        frame.undoButton.setEnabled(frame.undoManager.canUndo());
                        frame.redoButton.setEnabled(frame.undoManager.canRedo());
                        currentAction = null;
                    }
                }
            }
        });
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
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
            }
            selectedOverlay = overlay;
        }
    }
}
