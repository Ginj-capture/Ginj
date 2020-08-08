package info.ginj.ui.layout;

import javax.swing.*;
import java.awt.*;

/**
 * A layout making all added buttons the same size, dictated by the largest one.
 * From https://stackoverflow.com/questions/11536089/making-all-button-size-same
 *
 * @author Santhosh Kumar - santhosh@in.fiorano.com
 */
public class ButtonLayout implements LayoutManager, SwingConstants {

    private int gap;
    private int alignment;

    public ButtonLayout() {
        setAlignment(RIGHT);
        setGap(2);
    }

    public ButtonLayout(int gap) {
        this(RIGHT, gap);
    }

    public ButtonLayout(int alignment, int gap) {
        setAlignment(alignment);
        setGap(gap);
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    private Dimension[] dimensions(Component[] children) {
        int maxWidth = 0;
        int maxHeight = 0;
        int visibleCount = 0;
        Dimension componentPreferredSize;

        for (Component child : children) {
            if (child.isVisible()) {
                componentPreferredSize = child.getPreferredSize();
                maxWidth = Math.max(maxWidth, componentPreferredSize.width);
                maxHeight = Math.max(maxHeight, componentPreferredSize.height);
                visibleCount++;
            }
        }

        int usedWidth = 0;
        int usedHeight = 0;

        switch (alignment) {
            case LEFT, RIGHT -> {
                usedWidth = maxWidth * visibleCount + gap * (visibleCount - 1);
                usedHeight = maxHeight;
            }
            case TOP, BOTTOM -> {
                usedWidth = maxWidth;
                usedHeight = maxHeight * visibleCount + gap * (visibleCount - 1);
            }
        }

        return new Dimension[]{
                new Dimension(maxWidth, maxHeight),
                new Dimension(usedWidth, usedHeight),};
    }

    public void layoutContainer(Container container) {

        Insets insets = container.getInsets();
        int width = container.getWidth() - (insets.left + insets.right);
        int height = container.getHeight() - (insets.top + insets.bottom);

        Component[] children = container.getComponents();
        Dimension[] dim = dimensions(children);

        int maxWidth = dim[0].width;
        int maxHeight = dim[0].height;
        int usedWidth = dim[1].width;
        int usedHeight = dim[1].height;

        for (int i = 0, c = children.length; i < c; i++) {
            if (children[i].isVisible()) {
                switch (alignment) {
                    case LEFT -> children[i].setBounds(
                            insets.left + (maxWidth + gap) * i,
                            insets.top,
                            maxWidth,
                            maxHeight);
                    case TOP -> children[i].setBounds(
                            insets.left + ((width - maxWidth) / 2),
                            insets.top + (maxHeight + gap) * i,
                            maxWidth,
                            maxHeight);
                    case RIGHT -> children[i].setBounds(
                            width - insets.right - usedWidth + (maxWidth + gap) * i,
                            insets.top,
                            maxWidth,
                            maxHeight);
                    case BOTTOM -> children[i].setBounds(
                            insets.left + (maxWidth + gap) * i,
                            height - insets.bottom - usedHeight + (maxHeight + gap) * i,
//                                      insets.top,
                            maxWidth,
                            maxHeight);
                }
            }
        }
    }

    public Dimension minimumLayoutSize(Container c) {
        return preferredLayoutSize(c);
    }

    public Dimension preferredLayoutSize(Container container) {

        Insets insets = container.getInsets();

        Component[] children = container.getComponents();
        Dimension[] dim = dimensions(children);

        int usedWidth = dim[1].width;
        int usedHeight = dim[1].height;

        return new Dimension(
                insets.left + usedWidth + insets.right,
                insets.top + usedHeight + insets.bottom);
    }

    public void addLayoutComponent(String string, Component comp) {
    }

    public void removeLayoutComponent(Component c) {
    }

}