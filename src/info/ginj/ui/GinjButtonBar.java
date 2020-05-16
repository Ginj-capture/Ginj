package info.ginj.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * A Ginj Button Bar is made of a top area for tooltip and a bottom area for actual buttons (GinjButton)
 */
public class GinjButtonBar extends JPanel {
    private JLabel tooltipLabel;
    private JPanel buttonRow;

    public GinjButtonBar(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setBorder(new LineBorder(Color.RED));
        setLayout(new GridLayout(2, 1));

        final JPanel tooltipRow = new JPanel();
        //tooltipRow.setLayout(null);
        tooltipLabel = new JLabel();
        tooltipRow.add(tooltipLabel);
        super.add(tooltipRow);

        buttonRow = new JPanel();
        buttonRow.setLayout(new FlowLayout(FlowLayout.LEADING));
        super.add(buttonRow);
    }

    public GinjButtonBar() {
        this(true);
    }

    @Override
    public Component add(Component comp) {
        return buttonRow.add(comp);
    }

    public void setTooltipText(String text) {
        tooltipLabel.setText(text);
    }
}
