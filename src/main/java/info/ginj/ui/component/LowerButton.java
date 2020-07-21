package info.ginj.ui.component;

import javax.swing.*;
import java.awt.*;

/**
 * Button that is part of the button bar at the bottom (LowerButtonBar).
 * Hovering causes the helpLabel above to be updated and moved.
 */
public class LowerButton extends JButton {

    public LowerButton(String help, ImageIcon imageIcon) {
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
                    if (p instanceof LowerButtonBar) {
                        if (help == null || help.isEmpty()) {
                            help = " ";
                        }
                        ((LowerButtonBar) p).setHelpText(LowerButton.this, help);
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
