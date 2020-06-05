package info.ginj.tool;

import info.ginj.Coords;

import java.awt.*;

public abstract class RectangleOverlay extends Overlay {
    public static final int HANDLE_EXTERNAL_OFFSET = 3;

    protected Rectangle rectangle;

    public Overlay initialize(Point initialPoint, Color initialColor) {
        setColor(initialColor);
        rectangle = new Rectangle(initialPoint.x, initialPoint.y, 5,5);
        return this;
    }

    /**
     * Returns all handles of the component
     * By convention, handle index 0 is the release position when first drawing a component (arrow head or end of rectangle diagonal)
     * We then turn clockwise from there
     * @return
     */
    @Override
    public Point[] getHandles() {
        return new Point[]{
                new Point(rectangle.x + rectangle.width + HANDLE_EXTERNAL_OFFSET,
                          rectangle.y + rectangle.height + HANDLE_EXTERNAL_OFFSET),
                new Point(rectangle.x - HANDLE_EXTERNAL_OFFSET,
                          rectangle.y + rectangle.height + HANDLE_EXTERNAL_OFFSET),
                new Point(rectangle.x - HANDLE_EXTERNAL_OFFSET,
                          rectangle.y - HANDLE_EXTERNAL_OFFSET),
                new Point(rectangle.x + rectangle.width + HANDLE_EXTERNAL_OFFSET,
                          rectangle.y - HANDLE_EXTERNAL_OFFSET)
        };
    }

    /**
     * This method indicates that the given handle has moved to a new position
     * By convention, handle index 0 is the release position when first drawing a component (arrow head or end of rectangle diagonal)
     * We then turn clockwise from there
     * @param handleIndex
     * @param newPosition
     */
    @Override
    public void setHandlePosition(int handleIndex, Point newPosition) {
        switch (handleIndex) {
            case 0 -> {
                Coords.setX2(rectangle, newPosition.x - HANDLE_EXTERNAL_OFFSET, 0);
                Coords.setY2(rectangle, newPosition.y - HANDLE_EXTERNAL_OFFSET, 0);
            }
            case 1 -> {
                Coords.setX1(rectangle, newPosition.x + HANDLE_EXTERNAL_OFFSET, 0);
                Coords.setY2(rectangle, newPosition.y - HANDLE_EXTERNAL_OFFSET, 0);
            }
            case 2 -> {
                Coords.setX1(rectangle, newPosition.x + HANDLE_EXTERNAL_OFFSET, 0);
                Coords.setY1(rectangle, newPosition.y + HANDLE_EXTERNAL_OFFSET, 0);
            }
            case 3 -> {
                Coords.setX2(rectangle, newPosition.x - HANDLE_EXTERNAL_OFFSET, 0);
                Coords.setY1(rectangle, newPosition.y + HANDLE_EXTERNAL_OFFSET, 0);
            }
        }
    }


    public String toString() {
        return this.getClass().getSimpleName() + " for " + rectangle.toString();
    }
}
