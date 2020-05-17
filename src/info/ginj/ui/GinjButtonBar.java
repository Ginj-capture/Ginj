package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A Ginj Button Bar is made of a top area for tooltip and a bottom area for actual buttons (GinjButton),
 * plus another area at the bottom right for other components
 * When hovering over a GinjButton, the tooltip is filled accordingly and moves above the button
 */
public class GinjButtonBar extends JPanel {
    private JPanel tooltipPanel;
    private JLabel tooltipLabel;
    private JPanel buttonPanel;
    private JPanel otherCompPanel;
    private Dimension tooltipPanelSize;

    public GinjButtonBar(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setLayout(new GridBagLayout());

        // Prepare top left "tooltip" panel, dimensioned according to the JLabel it contains
        tooltipPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return tooltipPanelSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return tooltipPanelSize;
            }

            @Override
            public Dimension getMinimumSize() {
                return tooltipPanelSize;
            }

            @Override
            public Dimension getSize() {
                return tooltipPanelSize;
            }
        };
        tooltipLabel = new JLabel(" "); // Init it with a string so it takes some height when packing UI
        tooltipLabel.setName("GinjButtonTooltip"); // To be used as a selector in laf.xml
        tooltipPanel.add(tooltipLabel);

        // Compute tooltipLabel size with default layout manager of JPanel and remember it for Panel
        tooltipPanelSize = tooltipLabel.getPreferredSize();

        // OK, now let's switch to absolute positioning
        tooltipPanel.setLayout(null);
        tooltipLabel.setBounds(0,0,tooltipPanelSize.width, tooltipPanelSize.height);

        // Prepare bottom left "button" panel
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 0));

        // Prepare bottom right "other" panel
        otherCompPanel = new JPanel();
        otherCompPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        // And add them to the this frame
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0;
        c.gridy=0;
        c.insets = new Insets(0,5,0,5);
        c.fill = GridBagConstraints.BOTH;
        super.add(tooltipPanel, c);
        c = new GridBagConstraints();
        c.gridx=0;
        c.gridy=1;
        c.fill = GridBagConstraints.BOTH;
        super.add(buttonPanel, c);
        c = new GridBagConstraints();
        c.gridx=1;
        c.gridy=1;
        c.fill = GridBagConstraints.HORIZONTAL;
        super.add(otherCompPanel, c);
    }

    public GinjButtonBar() {
        this(true);
    }

    @Override
    public Component add(Component comp) {
        if (comp instanceof GinjButton) {
            return buttonPanel.add(comp);
        }
        else {
            return otherCompPanel.add(comp);
        }
    }

    /**
     * Display the given text tooltip above the given button
     */
    public void setTooltipText(GinjButton thisButton, String text) {
        tooltipLabel.setText(text);

        // Compute position of tooltip
        // get metrics from the JLabel
        FontMetrics metrics = tooltipLabel.getFontMetrics(tooltipLabel.getFont());
        int height = metrics.getHeight();
        int width = metrics.stringWidth(text) + 1; // Overestimate to avoid truncation
        // By default, center the label above the button
        final Rectangle thisButtonBounds = thisButton.getBounds();
        final int thisButtonCenterX = thisButtonBounds.x + thisButtonBounds.width / 2;
        // Check if end of label would go past right edge of panel
        int offsetX = 0;
        final int panelWidth = tooltipPanel.getWidth();
        int x2 = thisButtonCenterX + width/2;
        if (x2 > panelWidth) {
            offsetX = panelWidth - x2;
        }
        // Check if start of label would go past left edge of panel
        int x1 = thisButtonCenterX - width/2 + offsetX;
        if (x1 < 0) {
            x1 = 0;
        }
        tooltipLabel.setBounds(x1, 0, panelWidth - x1, height);
    }
}
