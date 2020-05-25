package info.ginj.tool.highlight;

import info.ginj.tool.RectangleOverlay;
import info.ginj.ui.Util;

import java.awt.*;

public class HighlightOverlay extends RectangleOverlay {
    @Override
    protected void drawComponent(Graphics g) {
        g.setColor(Util.getTranslucentColor(getColor()));
        g.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }
}
