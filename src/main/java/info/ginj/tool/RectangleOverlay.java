package info.ginj.tool;

import info.ginj.util.Coords;

import java.awt.*;

public abstract class RectangleOverlay extends Overlay {
    public static final int HANDLE_EXTERNAL_OFFSET = 2;
    private static final int MIN_RECT_WIDTH = 4;
    private static final int MIN_RECT_HEIGHT = 4;

    protected Rectangle rectangle;

    public Overlay initialize(Point initialPoint, Color initialColor) {
        setColor(initialColor);
        rectangle = new Rectangle(initialPoint.x, initialPoint.y, 5, 5);
        return this;
    }

    // Getter and setter for peristence

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    /**
     * Returns all handles of the component
     * By convention, handle index 0 is the release position when first drawing a component (arrow head or end of rectangle diagonal)
     * We then turn clockwise from there
     *
     * @return an array with all handles of this component
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
     *
     * @param handleIndex    the index of the handle to move
     * @param newPosition    the new position of that handle
     * @param skipSizeChecks if true, no size checks are made. This is required when moving a component by shifting
     *                       all its handles because during the loop on the handles, some corners may break the rules
     * @return the index of the (new) handle to move from now on. Normally = handleIndex param, except when changing quadrants
     */
    @Override
    public int setHandlePosition(int handleIndex, Point newPosition, boolean skipSizeChecks) {
        //Rectangle rOld = new Rectangle(rectangle);
        switch (handleIndex) {
            case 0 -> {
                // Bottom right handle
                if (skipSizeChecks) {
                    Coords.setX2(rectangle, newPosition.x - HANDLE_EXTERNAL_OFFSET, 0);
                    Coords.setY2(rectangle, newPosition.y - HANDLE_EXTERNAL_OFFSET, 0);
                }
                else {
                    if (!moveX2(newPosition)) handleIndex = oppositeHandleX(handleIndex);
                    if (!moveY2(newPosition)) handleIndex = oppositeHandleY(handleIndex);
                }
            }
            case 1 -> {
                // Bottom left handle
                if (skipSizeChecks) {
                    Coords.setX1(rectangle, newPosition.x + HANDLE_EXTERNAL_OFFSET, 0);
                    Coords.setY2(rectangle, newPosition.y - HANDLE_EXTERNAL_OFFSET, 0);
                }
                else {
                    if (!moveX1(newPosition)) handleIndex = oppositeHandleX(handleIndex);
                    if (!moveY2(newPosition)) handleIndex = oppositeHandleY(handleIndex);
                }
            }
            case 2 -> {
                // Top left handle
                if (skipSizeChecks) {
                    Coords.setX1(rectangle, newPosition.x + HANDLE_EXTERNAL_OFFSET, 0);
                    Coords.setY1(rectangle, newPosition.y + HANDLE_EXTERNAL_OFFSET, 0);
                }
                else {
                    if (!moveX1(newPosition)) handleIndex = oppositeHandleX(handleIndex);
                    if (!moveY1(newPosition)) handleIndex = oppositeHandleY(handleIndex);
                }
            }
            case 3 -> {
                // Top right handle
                if (skipSizeChecks) {
                    Coords.setX2(rectangle, newPosition.x - HANDLE_EXTERNAL_OFFSET, 0);
                    Coords.setY1(rectangle, newPosition.y + HANDLE_EXTERNAL_OFFSET, 0);
                }
                else {
                    if (!moveX2(newPosition)) handleIndex = oppositeHandleX(handleIndex);
                    if (!moveY1(newPosition)) handleIndex = oppositeHandleY(handleIndex);
                }
            }
        }
        //System.out.println("Rectangle old = " + rOld + " ; new = " + rectangle + " ; delta size = (" + (rectangle.width - rOld.width) + ", " + (rectangle.height - rOld.height) + ")");
        return handleIndex;
    }

    private int oppositeHandleX(int handleIndex) {
        return switch (handleIndex) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            default -> -1;
        };
    }

    private int oppositeHandleY(int handleIndex) {
        return switch (handleIndex) {
            case 0 -> 3;
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 0;
            default -> -1;
        };
    }

    /**
     * Moves the left side of the rectangle if it remains in acceptable boundaries. If not, don't apply change
     *
     * @param newPosition the new mouse position
     * @return true if we can keep moving this handle, or false if we changed quadrant and should start moving the opposite handle
     */
    private boolean moveX1(Point newPosition) {
        if (newPosition.x > rectangle.x + rectangle.width) {
            // Changing quadrant: switch to moving the right side
            return false;
        }
        else {
            final int maxX1 = rectangle.x + rectangle.width - MIN_RECT_WIDTH;
            final int newX1 = Math.min(newPosition.x + HANDLE_EXTERNAL_OFFSET, maxX1);
            final int currX1 = rectangle.x;
            if (newX1 <= maxX1 && currX1 != newX1) {
                Coords.setX1(rectangle, newX1, 0);
            }
            return true;
        }
    }

    /**
     * Moves the right side of the rectangle if it remains in acceptable boundaries. If not, don't apply change
     *
     * @param newPosition the new mouse position
     * @return true if we can keep moving this handle, or false if we changed quadrant and should start moving the opposite handle
     */
    private boolean moveX2(Point newPosition) {
        if (newPosition.x < rectangle.x) {
            // Changing quadrant: switch to moving the left side
            return false;
        }
        else {
            final int minX2 = rectangle.x + MIN_RECT_WIDTH;
            final int currX2 = rectangle.x + rectangle.width;
            final int newX2 = Math.max(newPosition.x - HANDLE_EXTERNAL_OFFSET, minX2);
            if (newX2 >= minX2 && currX2 != newX2) {
                // We're still allowed to move this handle
                Coords.setX2(rectangle, newX2, 0);
            }
            return true;
        }
    }

    private boolean moveY1(Point newPosition) {
        if (newPosition.y > rectangle.y + rectangle.height) {
            // Changing quadrant: switch to moving the bottom side
            return false;
        }
        else {
            final int maxY1 = rectangle.y + rectangle.height - MIN_RECT_HEIGHT;
            final int newY1 = Math.min(newPosition.y + HANDLE_EXTERNAL_OFFSET, maxY1);
            final int currY1 = rectangle.y;
            if (newY1 <= maxY1 && currY1 != newY1) {
                Coords.setY1(rectangle, newY1, 0);
            }
            return true;
        }
    }

    private boolean moveY2(Point newPosition) {
        if (newPosition.y < rectangle.y) {
            // Changing quadrant: switch to moving the top side
            return false;
        }
        else {
            final int minY2 = rectangle.y + MIN_RECT_HEIGHT;
            final int newY2 = Math.max(newPosition.y - HANDLE_EXTERNAL_OFFSET, minY2);
            final int currY2 = rectangle.y + rectangle.height;
            if (newY2 >= minY2 && currY2 != newY2) {
                Coords.setY2(rectangle, newY2, 0);
            }
            return true;
        }
    }


    public String toString() {
        return this.getClass().getSimpleName() + " for " + rectangle.toString();
    }
}
