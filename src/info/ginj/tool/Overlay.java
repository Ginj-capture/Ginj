package info.ginj.tool;

import com.jhlabs.image.GaussianFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

public abstract class Overlay extends JComponent {
    public static final RenderingHints ANTI_ALIASING_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    public static final int HANDLE_WIDTH = 8;
    public static final int HANDLE_HEIGHT = 8;
    public static final int NO_INDEX = -1;

    // State
    private Color color;
    private boolean selected = false;
    protected boolean editInProgress = true; // Creation is an edit.
    private BufferedImage shadowImage;


    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEditInProgress() {
        return editInProgress;
    }

    public void setEditInProgress(boolean editInProgress) {
        this.editInProgress = editInProgress;
        if (editInProgress) {
            shadowImage = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHints(ANTI_ALIASING_HINTS);

        // Draw shadow;
        if (!editInProgress) {
            if (shadowImage == null) {
                BufferedImageOp op = new GaussianFilter(8);
                BufferedImage maskImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                final Graphics2D maskImageG2D = maskImage.createGraphics();
                drawComponent(maskImageG2D, 3, 3);
                maskImageG2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 0.7f));
                maskImageG2D.setColor(Color.BLACK);
                maskImageG2D.fillRect(0, 0, getWidth(), getHeight());

                maskImageG2D.dispose();
                shadowImage = op.filter(maskImage, null);
            }
            g2d.drawImage(shadowImage, 0, 0, this);
        }

        // Draw component
        drawComponent(g2d, 0, 0);

        // Draw handles
        if (selected) {
            g.setColor(Color.BLACK);
            for (Point handle : getHandles()) {
                // TODO : better looking handles than just a square
                g.drawRect(handle.x - HANDLE_WIDTH/2, handle.y - HANDLE_HEIGHT/2, HANDLE_WIDTH, HANDLE_HEIGHT);
            }
        }
    }

    // Hit detection.
    // Note: this is similar to overriding contains(), except it is called only on click (and not on mouseover),
    public boolean containsPoint(Point p) {
        // Render the item in an image
        BufferedImage renderedImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) renderedImage.getGraphics();
        drawComponent(g2d, 0, 0);
        g2d.dispose();

        // Return true if the pixel at (x,y) is not transparent
        final int rgb = renderedImage.getRGB(p.x, p.y);
        return ((rgb & 0xFF000000) != 0);
    }

    public int getHandleIndexAt(Point p) {
        final Point[] handles = getHandles();
        for (int i = 0; i < handles.length; i++) {
            Point handle = handles[i];
            // Give tolerance: double handle sizes
            if (p.x >= handle.x - HANDLE_WIDTH/2 && p.x <= handle.x + HANDLE_WIDTH/2
                    && p.y >= handle.y - HANDLE_HEIGHT/2 && p.y <= handle.y + HANDLE_HEIGHT/2) {
                return i;
            }
        }
        return -1;
    }

    // TODO place here all logic regarding
    //  - mouse handling
    //  - selection
    //  - dragging of component, and just call moveTo or something
    //  - dragging of handles, and just call moveHandle(pointIndex, newPoint) upon move


    public abstract String getPresentationName();

    /**
     * This method should be called just after instantiating the component
     * @param initialPoint
     * @param initialColor
     * @return
     */
    public abstract Overlay initialize(Point initialPoint, Color initialColor);

    /**
     * This method should only draw the component itself
     *  @param g
     * @param xOffset
     * @param yOffset
     */
    public abstract void drawComponent(Graphics2D g, int xOffset, int yOffset);

    /**
     * Returns all handles of the component
     * By convention, when a component is first drawn, getHandles()[0] is the end of the drawing (arrowhead or second point of rectangle)
     * @return
     */
    public abstract Point[] getHandles();

    /**
     * This method indicates that the given handle has moved to a new position
     * By convention, when a component is first drawn, the end of the drawing (arrowhead or second point of rectangle) is returned with index 0
     * @param handleIndex
     * @param newPosition
     */
    public final void moveHandle(int handleIndex, Point newPosition) {
        if (handleIndex != NO_INDEX) {
            // This is a move of one handle
            setHandlePosition(handleIndex, newPosition);
            shadowImage = null;
        }
        else {
            System.err.printf("moveHandle with a handleIndex = NO_INDEX");
        }
    }

    protected abstract void setHandlePosition(int handleIndex, Point newPosition);

    public abstract boolean hasNoSize();

    public void moveDrawing(int deltaX, int deltaY) {
        // This is a drag'n'drop move => move all points
        final Point[] handles = getHandles();
        for (int i = 0; i < handles.length; i++) {
            setHandlePosition(i, new Point(handles[i].x + deltaX, handles[i].y + deltaY));
        }
        shadowImage = null;
    }
}