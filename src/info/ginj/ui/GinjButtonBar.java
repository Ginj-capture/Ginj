package info.ginj.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A Ginj Button Bar is made of a top area for help and a bottom area for actual buttons (GinjButton),
 * plus another area at the bottom right for other components
 * When hovering over a GinjButton, the help is filled accordingly and moves above the button
 */
public class GinjButtonBar extends JPanel {
    private JPanel helpPanel;
    private JLabel helpLabel;
    private JPanel buttonPanel;
    private JPanel otherCompPanel;
    private Dimension helpPanelSize;

    public GinjButtonBar() {
        this(true);
    }

    public GinjButtonBar(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setOpaque(true);
        setBorder(new EmptyBorder(2, 0, 5, 0));

        setLayout(new GridBagLayout());

        // Prepare top left "help" panel, dimensioned according to the JLabel it contains
        helpPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return helpPanelSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return helpPanelSize;
            }

            @Override
            public Dimension getMinimumSize() {
                return helpPanelSize;
            }

            @Override
            public Dimension getSize() {
                return helpPanelSize;
            }
        };
        helpLabel = new JLabel(" "); // Init it with a string so it takes some height when packing UI
        helpLabel.setBorder(null);
        final Insets insets = helpLabel.getInsets();
        insets.left = 0;
        insets.right = 0;
        insets.top = 0;
        insets.bottom = 0;
        helpPanel.add(helpLabel);

        // Compute helpLabel size with default layout manager of JPanel and remember it for Panel
        helpPanelSize = helpLabel.getPreferredSize();

        // OK, now let's switch to absolute positioning
        helpPanel.setLayout(null);
        helpLabel.setBounds(0, 0, helpPanelSize.width, helpPanelSize.height);

        // Prepare bottom left "button" panel
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 0));

        // Prepare bottom right "other" panel
        otherCompPanel = new JPanel();
        otherCompPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        // And add them to the this frame
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 5, 0, 5);
        c.fill = GridBagConstraints.BOTH;
        super.add(helpPanel, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        super.add(buttonPanel, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        super.add(otherCompPanel, c);
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
     * Display the given text help above the given button
     */
    public void setHelpText(GinjButton thisButton, String text) {
        helpLabel.setText(text);

        // Compute position of help
        // get metrics from the JLabel
        FontMetrics metrics = helpLabel.getFontMetrics(helpLabel.getFont());
        int height = metrics.getHeight();
        // TODO this seems to be wrong: Label is not centered and overflows...
        int width = metrics.stringWidth(text) + 1;
        // By default, center the label above the button
        final Rectangle thisButtonBounds = thisButton.getBounds();
        final int thisButtonCenterX = thisButtonBounds.x + thisButtonBounds.width / 2;
        // Check if end of label would go past right edge of panel
        int offsetX = 0;
        final int panelWidth = helpPanel.getWidth();
        int x2 = thisButtonCenterX + width / 2;
        if (x2 > panelWidth) {
            offsetX = panelWidth - x2;
        }
        // Check if start of label would go past left edge of panel
        int x1 = thisButtonCenterX - width / 2 + offsetX;
        if (x1 < 0) {
            x1 = 0;
        }
        helpLabel.setBounds(x1, 0, panelWidth - x1, height);
    }
}
