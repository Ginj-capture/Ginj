package info.ginj.tool.frame;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class FrameOverlay extends RectangleOverlay {
    @Override
    public String getPresentationName() {
        return "Frame";
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        g2d.drawRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height);
    }
}
