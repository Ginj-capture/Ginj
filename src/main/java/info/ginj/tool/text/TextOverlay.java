package info.ginj.tool.text;

import info.ginj.tool.RectangleOverlay;
import info.ginj.ui.CaptureEditingFrame;
import info.ginj.ui.ImageEditorPane;
import info.ginj.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class TextOverlay extends RectangleOverlay {

    // TODO Implement use of the following fields (popup to select font and color)
    protected Color textColor = UI.TEXT_TOOL_DEFAULT_FOREGROUND_COLOR;
    protected String fontName = "Arial";
    protected int fontSize = 18;
    protected int fontStyle = Font.PLAIN;
    // Plus these two as per request
    protected int strokeWidth = 6;
    protected Color backgroundColor = Color.WHITE;

    private JTextArea textArea;
    private ImageEditorPane imagePane;
    private CaptureEditingFrame frame;

    // Getters and setters required for XMLEncoder/XMLDecoder (de)serialization

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        textArea.setForeground(textColor);
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        // Store it
        this.fontName = fontName;
        // And update the text area
        textArea.setFont(new Font(fontName, fontStyle, fontSize));
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        // Store it
        this.fontSize = fontSize;
        // And update the text area
        textArea.setFont(new Font(fontName, fontStyle, fontSize));
    }

    public int getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(int fontStyle) {
        // Store it
        this.fontStyle = fontStyle;
        // And update the text area
        textArea.setFont(new Font(fontName, fontStyle, fontSize));
    }

    @Override
    public TextOverlay initialize(Point initialPoint, Color initialColor) {
        super.initialize(initialPoint, initialColor);
        textArea = new JTextArea();
        textArea.setForeground(initialColor);
        // TODO current font of overlay should be initialized with the Tool's default
        textArea.setFont(new Font(fontName, fontStyle, fontSize));

        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {

// TODO what happens after de/serialization of the overlay ?

                if (!isSelected()) {
                    // Set this one as selected, but also deselect others
                    imagePane.setSelectedOverlay(TextOverlay.this);
                }
            }
        });
        textArea.setFocusable(true);
        textArea.requestFocusInWindow();
        textArea.getDocument().addUndoableEditListener(
                e -> frame.addUndoableEdit(e.getEdit()));
        add(textArea);
        return this;
    }

    @Override
    public int setHandlePosition(int handleIndex, Point newPosition, boolean skipSizeChecks) {
        // As in Jing, moving or resizing a text overlay gives the focus to the TextArea
        if (!textArea.hasFocus()) {
            textArea.requestFocusInWindow();
        }
        return super.setHandlePosition(handleIndex, newPosition, skipSizeChecks);
    }

    @Override
    public String getPresentationName() {
        return "Text";
    }

    @Override
    public void drawComponent(Graphics2D g2d, int xOffset, int yOffset) {
        Rectangle textRectangle = new Rectangle(rectangle);
        textRectangle.grow(-10, -8);
        textArea.setBounds(textRectangle);
        textArea.setForeground(getColor());
        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(rectangle.x + 2 + xOffset, rectangle.y + 2 + yOffset, rectangle.width - 4, rectangle.height - 4, 8, 8);
        g2d.setColor(getColor());
        g2d.setStroke(new BasicStroke(strokeWidth));
        g2d.drawRoundRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height, 16, 16);
    }

    @java.beans.Transient
    public void setImagePane(ImageEditorPane imagePane) {
        this.imagePane = imagePane;
    }

    @java.beans.Transient
    public ImageEditorPane getImagePane() {
        return imagePane;
    }

    @java.beans.Transient
    public void setFrame(CaptureEditingFrame frame) {
        this.frame = frame;
    }

    @java.beans.Transient
    public CaptureEditingFrame getFrame() {
        return frame;
    }
}
