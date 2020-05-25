package info.ginj.tool;

import javax.swing.*;
import java.awt.*;

public abstract class Overlay extends JComponent {
    private Color color;
    private boolean selected = false;
    protected boolean dragging = false;

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
            for (Point handle : getHandles()) {
                // TODO : better looking handles than just a square
                g.drawRect(handle.x - 3, handle.y - 3, 6, 6);
            }
        }
    }

    // TODO place here all logic regarding
    //  - mouse handling
    //  - selection
    //  - dragging of component, and just call moveTo or something
    //  - dragging of handles, and just call moveHandle(pointIndex, newPoint) upon move


    /**
     * This method should be called just after instantiating the component
     *  @param initialPoint
     * @param initialColor
     * @return
     */
    public abstract Overlay initialize(Point initialPoint, Color initialColor);

    /**
     * This method should only draw the component itself
     *
     * @param g
     */
    protected abstract void drawComponent(Graphics g);

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
}