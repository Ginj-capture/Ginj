package info.ginj.tool.frame;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class FrameOverlay extends RectangleOverlay {
    @Override
    public void drawComponent(Graphics2D g2d) {
        g2d.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        if (!dragging) {
            // TODO drawShadow();
        }
    }
}
