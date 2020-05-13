package info.ginj;

import javax.swing.*;

import java.awt.*;

import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;

public class Ginj {

    public static void main(String[] args) {
        // Determine what the GraphicsDevice can support.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        //If translucent windows aren't supported, exit.
        if (!gd.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)) {
            System.out.println("Per-pixel translucency is not supported");
            System.exit(-1);
        }

        JFrame.setDefaultLookAndFeelDecorated(true);

        javax.swing.SwingUtilities.invokeLater(() -> {
            //OldMainUI ui = new OldMainUI();
            MainWindow ui = new MainWindow();
        });

    }
}
