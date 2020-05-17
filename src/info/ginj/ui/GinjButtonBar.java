package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A Ginj Button Bar is made of a top area for tooltip and a bottom area for actual buttons (GinjButton),
 * plus another area at the bottom right for other components
 * When hovering over a GinjButton, the tooltip is filled accordingly and moves above the button
 */
public class GinjButtonBar extends JPanel {
    private JLabel tooltipLabel;
    private JPanel buttonPanel;
    private JPanel otherCompPanel;

    public GinjButtonBar(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setLayout(new GridBagLayout());

        // Prepare top left "tooltip" panel
        final JPanel tooltipPanel = new JPanel();
        //tooltipPanel.setLayout(null);
        tooltipPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        tooltipLabel = new JLabel(" ");
        tooltipLabel.setName("ButtonKey"); // To be addressed in laf.xml
        tooltipPanel.add(tooltipLabel);

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

    public void setTooltipText(String text) {
        tooltipLabel.setText(text);
    }
}
