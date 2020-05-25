package info.ginj;

import java.awt.*;

/*
 * Coordinate utils
 */
public class Coords {

    // Only move left edge of rectangle, keeping other edges the same
    public static int setX1(Rectangle rectangle, int newX1, int currentOperation) {
        final int oldX1 = rectangle.x;
        final int oldX2 = rectangle.x + rectangle.width;
        if (newX1 <= oldX2) {
            // Normal case
            rectangle.setLocation(newX1, rectangle.y);
            rectangle.setSize(rectangle.width + oldX1 - newX1, rectangle.height);
            return currentOperation;
        }
        else {
            // Mouse has crossed the right edge
            rectangle.setLocation(oldX2, rectangle.y);
            rectangle.setSize(newX1 - oldX2, rectangle.height);
            return switch (currentOperation) {
                case Cursor.NW_RESIZE_CURSOR -> Cursor.NE_RESIZE_CURSOR;
                case Cursor.W_RESIZE_CURSOR -> Cursor.E_RESIZE_CURSOR;
                case Cursor.SW_RESIZE_CURSOR -> Cursor.SE_RESIZE_CURSOR;
                default -> currentOperation;
            };
        }
    }

    // Only move top edge of rectangle, keeping other edges the same
    public static int setY1(Rectangle rectangle, int newY1, int currentOperation) {
        final int oldY1 = rectangle.y;
        final int oldY2 = rectangle.y + rectangle.height;
        if (newY1 <= oldY2) {
            // Normal case
            rectangle.setLocation(rectangle.x, newY1);
            rectangle.setSize(rectangle.width, rectangle.height + oldY1 - newY1);
            return currentOperation;
        }
        else {
            // Mouse has crossed the bottom edge
            rectangle.setLocation(rectangle.x, oldY2);
            rectangle.setSize(rectangle.width, newY1 - oldY2);
            return switch (currentOperation) {
                case Cursor.NW_RESIZE_CURSOR -> Cursor.SW_RESIZE_CURSOR;
                case Cursor.N_RESIZE_CURSOR -> Cursor.S_RESIZE_CURSOR;
                case Cursor.NE_RESIZE_CURSOR -> Cursor.SE_RESIZE_CURSOR;
                default -> currentOperation;
            };
        }
    }

    // Only move right edge of rectangle, keeping other edges the same
    public static int setX2(Rectangle rectangle, int newX2, int currentOperation) {
        final int oldX1 = rectangle.x;
        final int oldX2 = rectangle.x + rectangle.width;
        if (newX2 >= oldX1) {
            // Normal case
            rectangle.setSize(rectangle.width - oldX2 + newX2, rectangle.height);
            return currentOperation;
        }
        else {
            // Mouse has crossed the left edge
            rectangle.setLocation(newX2, rectangle.y);
            rectangle.setSize(oldX1 - newX2, rectangle.height);
            return switch (currentOperation) {
                case Cursor.NE_RESIZE_CURSOR -> Cursor.NW_RESIZE_CURSOR;
                case Cursor.E_RESIZE_CURSOR -> Cursor.W_RESIZE_CURSOR;
                case Cursor.SE_RESIZE_CURSOR -> Cursor.SW_RESIZE_CURSOR;
                default -> currentOperation;
            };
        }
    }

    // Only move bottom edge of rectangle, keeping other edges the same
    public static int setY2(Rectangle rectangle, int newY2, int currentOperation) {
        final int oldY1 = rectangle.y;
        final int oldY2 = rectangle.y + rectangle.height;
        if (newY2 >= oldY1) {
            // Normal case
            rectangle.setSize(rectangle.width, rectangle.height - oldY2 + newY2);
            return currentOperation;
        }
        else {
            // Mouse has crossed the top edge
            rectangle.setLocation(rectangle.x, newY2);
            rectangle.setSize(rectangle.width, oldY1 - newY2);
            return switch (currentOperation) {
                case Cursor.SW_RESIZE_CURSOR -> Cursor.NW_RESIZE_CURSOR;
                case Cursor.S_RESIZE_CURSOR -> Cursor.N_RESIZE_CURSOR;
                case Cursor.SE_RESIZE_CURSOR -> Cursor.NE_RESIZE_CURSOR;
                default -> currentOperation;
            };
        }
    }

}
