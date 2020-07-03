package info.ginj.tool.arrow;

import info.ginj.tool.Overlay;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

public class ArrowOverlay extends Overlay {

    private Point start;
    private Point end;

    public Overlay initialize(Point initialPoint, Color initialColor) {
        setColor(initialColor);
        start = initialPoint;
        end = initialPoint;
        return this;
    }


    // Getters and setters required for XMLEncoder/XMLDecoder (de)serialization

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getEnd() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    @Override
    public String getPresentationName() {
        return "Arrow";
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        drawArrowLine(g2d, start.x + xOffset, start.y + yOffset, end.x + xOffset, end.y + yOffset);
    }

    /**
     * Returns all handles of the component. Handles are squares displayed over the selected overlay, providing ways to change its shape.
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
    public void setHandlePosition(int handleIndex, Point newPosition) {
        if (handleIndex == 0) end = newPosition;
        else start = newPosition;
    }


    /**
     * Draw an arrow line between two points.
     * from https://stackoverflow.com/a/27461352/13551878
     * @param g2d the graphics component.
     * @param x1 x-position of first point.
     * @param y1 y-position of first point.
     * @param x2 x-position of second point.
     * @param y2 y-position of second point.
     */
    private void drawArrowLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dx == 0 && dy == 0) return;

        double hyp = Math.sqrt(dx*dx + dy*dy);

        // The width of the arrow (the length of the base of the triangle, perpendicular to the shaft)
        // Normally 10, except for very small arrows - ENHANCEMENT
        int headWidth = Math.min(8, (int)hyp/3);
        // the length of the head (from the tip to the center of the base)
        int headLength = 2 * headWidth;

        double shaftLength = hyp - headLength;
        double ym = headWidth;
        double yn = -headWidth;
        double sin = dy / hyp;
        double cos = dx / hyp;
        double xn, xm;

        xm = shaftLength*cos - ym*sin + x1;
        ym = shaftLength*sin + ym*cos + y1;

        xn = shaftLength*cos - yn*sin + x1;
        yn = shaftLength*sin + yn*cos + y1;

        // Order: center of triangle, one side, end of arrow, other side
        float[] xpoints = {(int) ((x2+xm+xn)/3), (int) xm, x2, (int) xn};
        float[] ypoints = {(int) ((y2+ym+yn)/3), (int) ym, y2, (int) yn};

        // SHAFT
        // Thick Stroke with a round start
        g2d.setColor(getColor());
        g2d.setStroke(new BasicStroke(headWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // But end at the center of the head triangle to avoid a rounded angle at the tip of the head
        g2d.draw(new Line2D.Float(x1, y1, xpoints[0], ypoints[0]));

        // HEAD
        // Thinner Stroke so that the end is less rounded
        g2d.setStroke(new BasicStroke(headWidth/2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Draw a polygon including the center of the triangle to achieve a slimmer look than a plain triangle
        GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xpoints.length);
        polyline.moveTo(xpoints[0], ypoints[0]);
        polyline.lineTo(xpoints[1], ypoints[1]);
        polyline.lineTo(xpoints[2], ypoints[2]);
        polyline.lineTo(xpoints[3], ypoints[3]);
        polyline.closePath();
        // Fill the interior
        g2d.fill(polyline);
        // And draw the outline
        g2d.draw(polyline);
    }

    @Override
    public String toString() {
        return "Arrow from " + start + " to " + end;
    }

}
