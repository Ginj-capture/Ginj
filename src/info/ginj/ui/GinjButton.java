package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

public class GinjButton extends JButton {

    public GinjButton(String tooltip, ImageIcon imageIcon) {
        super(imageIcon);

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setParentTooltip(tooltip);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setParentTooltip("");
            }

            private void setParentTooltip(String tooltip) {
                Component p = getParent();
                while (p != null) {
                    if (p instanceof GinjButtonBar) {
                        ((GinjButtonBar) p).setTooltipText(tooltip);
                        break;
                    }
                    else {
                        p = p.getParent();
                    }
                }
            }
        });

    }
}
