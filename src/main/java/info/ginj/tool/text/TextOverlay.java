package info.ginj.tool.text;

import info.ginj.tool.RectangleOverlay;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.Transient;

public class TextOverlay extends RectangleOverlay {
    private static final Logger logger = LoggerFactory.getLogger(TextOverlay.class);

    public static final int DEFAULT_FONT_SIZE = 18;

    // TODO Implement use of the following fields (popup to select font and color)
    protected Color textColor = UI.TEXT_TOOL_DEFAULT_FOREGROUND_COLOR;
    protected String fontName = "Arial";
    protected int fontStyle = Font.PLAIN;
    protected Color backgroundColor = Color.WHITE;
    protected int strokeWidth = 6;
    // These are used:
    protected int fontSize = DEFAULT_FONT_SIZE;
    private int backgroundAlpha = 255;

    private JTextArea textArea;
    private FocusAdapter selectOnFocusGainedFocusAdapter;
    private JPanel buttonBar;


    // REMINDER: Canonical constructor and getters and setters **FOR ALL FIELDS** are **REQUIRED** for XMLEncoder/XMLDecoder (de)serialization

    public TextOverlay() {
        super();
    }
    public JTextArea getTextArea() {
        return textArea;
    }
    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }
    @Transient
    public FocusAdapter getSelectOnFocusGainedFocusAdapter() {
        return selectOnFocusGainedFocusAdapter;
    }
    @Transient
    public void setSelectOnFocusGainedFocusAdapter(FocusAdapter selectOnFocusGainedFocusAdapter) {
        this.selectOnFocusGainedFocusAdapter = selectOnFocusGainedFocusAdapter;
    }
    @Transient
    public JPanel getButtonBar() {
        return buttonBar;
    }
    @Transient
    public void setButtonBar(JPanel buttonBar) {
        this.buttonBar = buttonBar;
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

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public void setBackgroundAlpha(int backgroundAlpha) {
        this.backgroundAlpha = backgroundAlpha;
        // TODO highlight button if transparent (not in the listener because it can be restored from disk)
        clearShadow();
        repaint();
    }
    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    @Override
    public TextOverlay initialize(Point initialPoint, Color initialColor) {
        super.initialize(initialPoint, initialColor);
        addTextArea(initialColor);
        setUp();
        return this;
    }

    @Override
    public void setUp() {
        super.setUp();
        selectOnFocusGainedFocusAdapter = new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (!isSelected()) {
                        // Set this one as selected, but also deselect others
                        getFrame().getImagePane().setSelectedOverlay(TextOverlay.this);
                    }
                }
        };
        textArea.addFocusListener(selectOnFocusGainedFocusAdapter);
        addButtonBar();
    }

    @Override
    public void tearDown() {
        remove(buttonBar);
        buttonBar = null;
        if (selectOnFocusGainedFocusAdapter != null) {
            textArea.removeFocusListener(selectOnFocusGainedFocusAdapter);
        }
        super.tearDown();
    }

    private void addTextArea(Color initialColor) {
        textArea = new JTextArea();
        textArea.setForeground(initialColor);
        // TODO current font of overlay should be initialized with the Tool's default
        textArea.setFont(new Font(fontName, fontStyle, fontSize));

        textArea.setFocusable(true);
        textArea.requestFocusInWindow();
        textArea.getDocument().addUndoableEditListener(
                e -> getFrame().addUndoableEdit(e.getEdit()));
        add(textArea);
    }

    private void addButtonBar() {
        buttonBar = new JPanel();
        buttonBar.setName("OverlayButtonBarPanel"); // to be addressed in synth.xml);
        buttonBar.setLayout(new FlowLayout(FlowLayout.LEADING, 2, 2));

        JButton smallerFontButton = new JButton(UI.createIcon(getClass().getResource("/img/icon/font_smaller.png"), 17, 17, UI.ICON_ENABLED_COLOR));
        smallerFontButton.addActionListener(e -> setFontSize(Math.max(getFontSize() - 2, 6)));
        Dimension smallerFontButtonSize = smallerFontButton.getPreferredSize();
        buttonBar.add(smallerFontButton);

        JButton largerFontButton = new JButton(UI.createIcon(getClass().getResource("/img/icon/font_larger.png"), 17, 17, UI.ICON_ENABLED_COLOR));
        largerFontButton.addActionListener(e -> setFontSize(getFontSize() + 2));
        Dimension largerFontButtonSize = smallerFontButton.getPreferredSize();
        buttonBar.add(largerFontButton);

        JButton glassButton = new JButton(UI.createIcon(getClass().getResource("/img/icon/glass.png"), 17, 17, UI.ICON_ENABLED_COLOR));
        glassButton.addActionListener(e -> setBackgroundAlpha(255 - getBackgroundAlpha()));
        Dimension glassButtonSize = smallerFontButton.getPreferredSize();
        buttonBar.add(glassButton);
        if (backgroundAlpha != 0) {
            //glassButtonSize.setSelected(true);
        }

        buttonBar.setBounds(0, 0,
                2
                        + smallerFontButtonSize.width + 2
                        + largerFontButtonSize.width + 2
                        + glassButtonSize.width + 2
                , Misc.getMaxInt(smallerFontButtonSize.height, largerFontButtonSize.height, glassButtonSize.height)
                        + 4);
        buttonBar.setVisible(false);
        add(buttonBar);
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
        if (backgroundAlpha == 0) {
            textArea.setOpaque(false);
            if (isSelected()) {
                // Semi transparent border to be able to grab and move the overlay
                g2d.setColor(new Color(128,128,128,64));
                g2d.setStroke(new BasicStroke(strokeWidth));
                g2d.drawRoundRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height, 16, 16);
            }
        }
        else {
            textArea.setOpaque(true);
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(rectangle.x + 2 + xOffset, rectangle.y + 2 + yOffset, rectangle.width - 4, rectangle.height - 4, 8, 8);
            g2d.setColor(getColor());
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawRoundRect(rectangle.x + xOffset, rectangle.y + yOffset, rectangle.width, rectangle.height, 16, 16);
        }
    }

    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (backgroundAlpha == 0) {
            clearShadow();
        }
    }

    @Override
    public void setButtonBarVisible(boolean visible) {
        super.setButtonBarVisible(visible);
        if(buttonBar == null) {
            addButtonBar();
        }
        buttonBar.setVisible(visible);
        if (visible) {
            Rectangle bounds = buttonBar.getBounds();
            Point topLeft = getOverlayTopLeftBasedOnHandles();
            int x = topLeft.x + 2;
            int y = topLeft.y - bounds.height - 1;
            if (y < 0) {
                // Hmmm bar will be clipped by "-y" pixels at the top. See if we can position it below the overlay
                Point bottomLeft = getOverlayBottomLeftBasedOnHandles();
                if (getParent() == null) {
                    logger.error("getParent() is null in setButtonBarVisible()");
                }
                else {
                    int clippingBelow = (bottomLeft.y + 2 + bounds.height) - getParent().getHeight();
                    if (clippingBelow < -y) {
                        // there is less (or no) clipping below => position below
                        y = bottomLeft.y + 2;
                    }
                }
            }
            bounds.setLocation(x, y);
            buttonBar.setBounds(bounds);
        }
    }

}
