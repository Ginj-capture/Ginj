package info.ginj.tool.oval;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class OvalOverlay extends RectangleOverlay {
    @Override
    public String getPresentationName() {
        return "Oval";
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        g2d.setColor(getColor());
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height);
    }
}
