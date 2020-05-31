package info.ginj.tool.text;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class TextOverlay extends RectangleOverlay {

    // TODO there should be a JTextArea inside
    // So should this extends JPanel instead

    @Override
    public String getPresentationName() {
        return "Text";
    }

    @Override
    public void drawComponent(Graphics2D g2d) {
        g2d.setColor(getColor());
        g2d.drawRoundRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, 4, 4);
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(rectangle.x+2, rectangle.y+2, rectangle.width-4, rectangle.height-4, 4, 4);
        if (!dragging) {
            // TODO drawShadow();
        }
    }
}
