package info.ginj.tool;

import java.awt.*;

public abstract class RectangleOverlay extends Overlay {
    protected Rectangle rectangle;

    public Overlay initialize(Point initialPoint, Color initialColor) {
        setColor(initialColor);
        rectangle = new Rectangle(initialPoint.x, initialPoint.y, 5,5);
        return this;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName() + " for " + rectangle.toString();
    }

    /**
     * Returns all handles of the component
     * By convention, when a component is first drawn, getHandles()[0] is the end of the drawing (arrowhead or second point of rectangle)
     * @return
     */
    @Override
    public Point[] getHandles() {
        return new Point[]{
                new Point(rectangle.x - 3, rectangle.y - 3),
                new Point(rectangle.x - 3, rectangle.y + rectangle.height + 3),
                new Point(rectangle.x + rectangle.width + 3, rectangle.y + rectangle.height + 3),
                new Point(rectangle.x + rectangle.width + 3, rectangle.y - 3)
        };
    }

    /**
     * This method indicates that the given handle has moved to a new position
     * By convention, when a component is first drawn, the end of the drawing (arrowhead or second point of rectangle) is returned with index 0
     * @param handleIndex
     * @param newPosition
     */
    @Override
    public void setHandlePosition(int handleIndex, Point newPosition) {
        // Do it
    }

    @Override
    public boolean hasNoSize() {
        return (rectangle.getWidth() == 0 && rectangle.getHeight() == 0);
    }
}
