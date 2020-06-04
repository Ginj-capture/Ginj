package info.ginj.ui;

import javax.swing.*;
import java.awt.*;

public class GinjButton extends JButton {

    public GinjButton(String help, ImageIcon imageIcon) {
        super(imageIcon);
        setName("GinjButton"); // To be addressed in laf.xml

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
                    if (p instanceof GinjButtonBar) {
                        if (help == null || help.isEmpty()) {
                            help = " ";
                        }
                        ((GinjButtonBar) p).setHelpText(GinjButton.this, help);
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
