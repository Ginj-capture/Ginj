package info.ginj.tool.highlight;

import info.ginj.tool.RectangleOverlay;
import info.ginj.util.UI;

import java.awt.*;

public class HighlightOverlay extends RectangleOverlay {
    @Override
    public String getPresentationName() {
        return "Highlight";
    }

    /**
     * Simplified version as highlights don't have a shadow
     * @param point the point to test
     * @return
     */
    @Override
    public boolean containsPoint(Point point) {
        // First see if we're in a handle
        if (isSelected() && getHandleIndexAt(point) != NO_INDEX) return true;

        // Otherwise, see if we're in the rectangle
        return rectangle.contains(point);
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        g2d.setColor(UI.getTranslucentColor(getColor()));
        g2d.fillRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height);
    }

    @Override
    protected boolean mustDrawShadow() {
        return false;
    }
}
