package info.ginj.tool;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Overlay extends JComponent {
    // State
    private Color color;
    private boolean selected = false;
    protected boolean dragging = false;

    // Caching
    private BufferedImage renderedImage = null;

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

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawComponent(g);
        if (selected) {
            g.setColor(Color.BLACK);
            for (Point handle : getHandles()) {
                // TODO : better looking handles than just a square
                g.drawRect(handle.x - 3, handle.y - 3, 6, 6);
            }
        }
    }

    // Hit detection.
    // Note: this is similar to overriding contains(), except it is called only on click (and not on mouseover),
    public boolean isInComponent(Point p) {
        if (renderedImage == null) {
            // Render the item in an image
            renderedImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
            Graphics g = renderedImage.getGraphics();
            drawComponent(g);
            g.dispose();
        }
        // Return true if the pixel at (x,y) is not transparent
        final int rgb = renderedImage.getRGB(p.x, p.y);
        return ((rgb & 0xFF000000) != 0);
    }


    protected void clearRenderedCache() {
        renderedImage = null;
    }


    // TODO place here all logic regarding
    //  - mouse handling
    //  - selection
    //  - dragging of component, and just call moveTo or something
    //  - dragging of handles, and just call moveHandle(pointIndex, newPoint) upon move


    /**
     * This method should be called just after instantiating the component
     * @param initialPoint
     * @param initialColor
     * @return
     */
    public abstract Overlay initialize(Point initialPoint, Color initialColor);

    /**
     * This method should only draw the component itself
     *
     * @param g
     */
    public abstract void drawComponent(Graphics g);

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
    public abstract void moveHandle(int handleIndex, Point newPosition);

    public abstract boolean hasNoSize();
}