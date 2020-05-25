package info.ginj.tool.frame;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class FrameOverlay extends RectangleOverlay {
    @Override
    protected void drawComponent(Graphics g) {
        g.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        if (!dragging) {
            // TODO drawShadow();
        }
    }
}
