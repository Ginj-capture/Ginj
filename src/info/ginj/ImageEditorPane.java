package info.ginj;

import info.ginj.tool.Overlay;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ImageEditorPane extends JLayeredPane {
    private final CaptureEditingFrame frame;
    private final BufferedImage capturedImg;
    private final Dimension capturedImgSize;

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

    /*
     * TODO add click filter like on star window
     */
    private void addMouseEditingBehaviour() {
        final JLayeredPane imagePanel = this;
        MouseInputListener mouseListener = new MouseInputAdapter() {
            private Overlay selectedOverlay;
            Point clicked;

            public void mousePressed(MouseEvent e) {
                clicked = e.getPoint();

                // See if we clicked in a component
                boolean found = false;
                // Iterate in reverse direction to check closest first
                // Note: JLayeredPane guarantees components are returned based on their layer order. See implementation of JLayeredPane.highestLayer()
                for (int i = imagePanel.getComponentCount() - 1; i >= 0; i--) {
                    final Component component = imagePanel.getComponent(i);
                    if (component instanceof Overlay) {
                        Overlay overlay = (Overlay) component;
                        overlay.setSelected(false);
                        if (!found) {
                            if (overlay.isInComponent(clicked)) {
                                overlay.setSelected(true);
                                found = true;
                            }
                        }
                    }
                    else {
                        System.out.println("Encountered unexpected component: " + component);
                    }
                }
                if (!found) {
                    // No component selected. Create a new one
                    selectedOverlay = frame.currentTool.createComponent(clicked, frame.currentColor);
                    selectedOverlay.setBounds(0, 0, capturedImgSize.width, capturedImgSize.height);
                    AddOverlayAction action = new AddOverlayAction(selectedOverlay, imagePanel);
                    action.execute();
                    frame.undoManager.undoableEditHappened(new UndoableEditEvent(imagePanel, action));
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
                    imagePanel.remove(selectedOverlay);
                }
                else {
                    // TODO Selection and modification

                    // Add creation to undo stack
                    ModifyOverlayAction action = new ModifyOverlayAction(selectedOverlay, imagePanel);
                    action.execute();
                    frame.undoManager.undoableEditHappened(new UndoableEditEvent(imagePanel, action));
                    frame.undoButton.setEnabled(frame.undoManager.canUndo());
                    frame.redoButton.setEnabled(frame.undoManager.canRedo());
                }
            }
        };
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }


    static class ModifyOverlayAction extends AbstractUndoableEdit {
        private final Overlay overlay;
        private final JLayeredPane panel;

        public ModifyOverlayAction(Overlay overlay, JLayeredPane panel) {
            this.overlay = overlay;
            this.panel = panel;
        }

        public String getPresentationName() {
            return "Modify " + overlay.getName().toLowerCase();
        }

        public void execute() {
//            overlay.setSelected(true);
//            panel.add(overlay, Integer.valueOf(panel.getComponentCount()));
        }


        public void undo() {
            super.undo();
            // undo the change
        }

        public void redo() {
            super.redo();
            execute();
        }

    }

    static class AddOverlayAction extends AbstractUndoableEdit {
        private final Overlay overlay;
        private final JLayeredPane panel;

        public AddOverlayAction(Overlay overlay, JLayeredPane panel) {
            this.overlay = overlay;
            this.panel = panel;
        }

        public String getPresentationName() {
            return "Create " + overlay.getName().toLowerCase();
        }

        public void execute() {
            overlay.setSelected(true);
            panel.add(overlay, Integer.valueOf(panel.highestLayer() + 1));
        }

        public void undo() {
            super.undo();
            panel.remove(overlay);
        }

        public void redo() {
            super.redo();
            execute();
        }
    }

}
