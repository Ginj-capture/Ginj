package info.ginj.tool.text;

import info.ginj.tool.RectangleOverlay;

import java.awt.*;

public class TextOverlay extends RectangleOverlay {

    // TODO there should be a JTextArea inside
    // So should Overlay extend JPanel instead of JComponent ?

    @Override
    public String getPresentationName() {
        return "Text";
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(rectangle.x+2 + xOffset, rectangle.y+2 + yOffset, rectangle.width-4, rectangle.height-4, 8, 8);
        g2d.setColor(getColor());
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRoundRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height, 16, 16);
    }
}
