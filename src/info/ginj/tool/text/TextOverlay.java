package info.ginj.tool.text;

import info.ginj.tool.RectangleOverlay;
import info.ginj.ui.Util;

import javax.swing.*;
import java.awt.*;

public class TextOverlay extends RectangleOverlay {
    Color textColor = Util.TEXT_TOOL_DEFAULT_FOREGROUND_COLOR;

    private JTextPane textPane;

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }


    @Override
    public TextOverlay initialize(Point initialPoint, Color initialColor) {
        super.initialize(initialPoint, initialColor);
        textPane = new JTextPane();
        textPane.setBackground(Util.TEXTFIELD_BACKGROUND_COLOR);
        textPane.setForeground(initialColor);
        textPane.setSelectionColor(Util.TEXTFIELD_SELECTION_BACKGROUND_COLOR);
        textPane.setFont(new Font("Arial", Font.PLAIN, 18));
textPane.setText("ABCDEFGHIJKLMNO\n" +
        "abcdefghijklmno\n" +
        "1234567890\n" +
        "&!§$ù%\n" +
        "selected");
        add(textPane);
        return this;
    }

    @Override
    public String getPresentationName() {
        return "Text";
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        Rectangle textRectangle = new Rectangle(rectangle);
        textRectangle.grow(-10, -8);
        textPane.setBounds(textRectangle);
        textPane.setForeground(getColor());
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(rectangle.x+2 + xOffset, rectangle.y+2 + yOffset, rectangle.width-4, rectangle.height-4, 8, 8);
        g2d.setColor(getColor());
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRoundRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height, 16, 16);
    }
}
