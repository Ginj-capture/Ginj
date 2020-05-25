package info.ginj.tool.arrow;

import info.ginj.tool.Overlay;

import java.awt.*;

public class ArrowOverlay extends Overlay {

    private Point start;
    private Point end;

    public Overlay initialize(Point initialPoint, Color initialColor) {
        setColor(initialColor);
        start = initialPoint;
        end = initialPoint;
        return this;
    }

    @Override
    protected void drawComponent(Graphics g) {
        g.setColor(getColor());
        g.drawLine(start.x, start.y, end.x, end.y);
        drawArrowLine(g, start.x, start.y, end.x, end.y, 20, 20);
        if (!dragging) {
            // TODO draw shadow;
        }
    }

    /**
     * Returns all handles of the component
     * By convention, when a component is first drawn, getHandles()[0] is the end of the drawing (arrowhead or second point of rectangle)
     * @return
     */
    @Override
    public Point[] getHandles() {
        return new Point[] {end, start};
    }

    /**
     * This method indicates that the given handle has moved to a new position
     * By convention, when a component is first drawn, the end of the drawing (arrowhead or second point of rectangle) is returned with index 0
     * @param handleIndex
     * @param newPosition
     */
    @Override
    public void moveHandle(int handleIndex, Point newPosition) {
        if (handleIndex == 0) end = newPosition;
        else start = newPosition;
    }


    /**
     * Draw an arrow line between two points.
     * from https://stackoverflow.com/a/27461352/13551878
     *
     * @param g the graphics component.
     * @param x1 x-position of first point.
     * @param y1 y-position of first point.
     * @param x2 x-position of second point.
     * @param y2 y-position of second point.
     * @param d  the width of the arrow.
     * @param h  the height of the arrow.
     */
    private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;

        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};

        g.drawLine(x1, y1, x2, y2);
        g.fillPolygon(xpoints, ypoints, 3);
    }
}
