package info.ginj.tool.highlight;

import info.ginj.tool.RectangleOverlay;
import info.ginj.ui.Util;

import java.awt.*;

public class HighlightOverlay extends RectangleOverlay {
    @Override
    public String getPresentationName() {
        return "Highlight";
    }

    @Override
    public void drawComponent(Graphics2D g2d) {
        g2d.setColor(Util.getTranslucentColor(getColor()));
        g2d.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }
}
