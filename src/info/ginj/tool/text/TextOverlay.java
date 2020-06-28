package info.ginj.tool.text;

import com.google.gson.annotations.Expose;
import info.ginj.CaptureEditingFrame;
import info.ginj.ImageEditorPane;
import info.ginj.tool.RectangleOverlay;
import info.ginj.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class TextOverlay extends RectangleOverlay {
    // Actual fields to persist and restore
    @Expose
    protected String text;

    // TODO Implement use of the following fields are not used yet
    @Expose
    protected Color textColor = Util.TEXT_TOOL_DEFAULT_FOREGROUND_COLOR;
    @Expose
    protected String fontName;
    @Expose
    protected int fontSize;
    @Expose
    protected int fontStyle;

    private JTextPane textPane;
    private ImageEditorPane imagePane;
    private CaptureEditingFrame frame;

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        textPane.setForeground(textColor);
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        // Store it
        this.fontName = fontName;
        // And update the textPane
        textPane.setFont(new Font(fontName, fontStyle, fontSize));
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        // Store it
        this.fontSize = fontSize;
        // And update the textPane
        textPane.setFont(new Font(fontName, fontStyle, fontSize));
    }

    public int getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(int fontStyle) {
        // Store it
        this.fontStyle = fontStyle;
        // And update the textPane
        textPane.setFont(new Font(fontName, fontStyle, fontSize));
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected) {
            if (!textPane.hasFocus()) {
                textPane.requestFocusInWindow();
            }
        }
    }

    @Override
    public TextOverlay initialize(Point initialPoint, Color initialColor) {
        super.initialize(initialPoint, initialColor);
        textPane = new JTextPane();
        textPane.setForeground(initialColor);
        // TODO current font of overlay should be a variable, initialized with the Tool's default
        textPane.setFont(new Font("Arial", Font.PLAIN, 18));

        textPane.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!isSelected()) {
                    // Set this one as selected, but also deselect others
                    imagePane.setSelectedOverlay(TextOverlay.this);
                }
            }
        });
        textPane.setFocusable(true);
        textPane.requestFocusInWindow();
        textPane.getDocument().addUndoableEditListener(
                e -> frame.addUndoableEdit(e.getEdit()));
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
        g2d.fillRoundRect(rectangle.x + 2 + xOffset, rectangle.y + 2 + yOffset, rectangle.width - 4, rectangle.height - 4, 8, 8);
        g2d.setColor(getColor());
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRoundRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height, 16, 16);
    }

    @Override
    public void beforeSerialize() {
        super.beforeSerialize();

        // Extract information from the textPane
        text = textPane.getText();
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
    }

    public void setImagePane(ImageEditorPane imagePane) {
        this.imagePane = imagePane;
    }

    public ImageEditorPane getImagePane() {
        return imagePane;
    }

    public void setFrame(CaptureEditingFrame frame) {
        this.frame = frame;
    }

    public CaptureEditingFrame getFrame() {
        return frame;
    }
}
