package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Button that is part of the button bar at the bottom (GinjLowerButtonBar).
 * Hovering causes the helpLabel above to be updated and moved.
 */
public class GinjLowerButton extends JButton {

    public GinjLowerButton(String help, ImageIcon imageIcon) {
        super(imageIcon);

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setParentHelp(help);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setParentHelp(null);
            }

            private void setParentHelp(String help) {
                Component p = getParent();
                while (p != null) {
                    if (p instanceof GinjLowerButtonBar) {
                        if (help == null || help.isEmpty()) {
                            help = " ";
                        }
                        ((GinjLowerButtonBar) p).setHelpText(GinjLowerButton.this, help);
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
