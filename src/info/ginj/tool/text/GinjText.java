package info.ginj.tool.text;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class GinjText extends RectangleOverlay {

    // TODO there should be a JTextArea inside
    // So should this extends JPanel instead

    @Override
    protected void drawComponent(Graphics g) {
        g.setColor(getColor());
        g.drawRoundRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, 4, 4);
        g.setColor(Color.WHITE);
        g.fillRoundRect(rectangle.x+2, rectangle.y+2, rectangle.width-4, rectangle.height-4, 4, 4);
        if (!dragging) {
            // TODO drawShadow();
        }
    }
}
