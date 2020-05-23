package info.ginj;

/*
TODO Features :
 - Fix toolbar buttons L&F
 - Do overlays
 - Implement fixed ratio to 16:9 (shift-drag) or 4:3 (ctrl-drag) + snap to resp  640x360,800x450,960x540,1280x720 or 320x240,400x300,640x480,800x600,1024x768 / ENHANCEMENT: in 4:3 1280x960
 - Implement Window detection using JNA
 - Implement "exports"
 - Implement local storage

TODO UI:
 - Finalize Look and feel
 - Fix scrollbar corner + thumb icon + colors + gap
 - Upscale sun and sun-rays so that runtime downscale provides an anti-aliasing, or better yet draw it by code (gradients etc)
 - Build 3 main buttons at runtime based on circle + icons (downscale provides an anti-aliasing)
 - Make a yellowish icons for disabled buttons

TODO Cleanup:
 - Remove EASynth resource dir
 - Remove useless EASynth classes, if any
 - Remove useless icons ?

*/

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;

import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;

public class Ginj {

    public static final String LAF_XML = "laf.xml";

    public static final int ERR_STATUS_TRANSPARENCY = -1;
    public static final int ERR_STATUS_LAF = -2;
    public static final int ERR_STATUS_LOAD_IMG = -3;
    public static final int ERR_STATUS_OK = 0;

    public static void main(String[] args) {
        // Determine what the GraphicsDevice can support.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        //If translucent windows aren't supported, exit.
        if (!gd.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)) {
            System.err.println("Per-pixel translucency is not supported");
            System.exit(ERR_STATUS_TRANSPARENCY);
        }

        SynthLookAndFeel ginjLookAndFeel = new SynthLookAndFeel();
        try {
            ginjLookAndFeel.load(Ginj.class.getResourceAsStream(LAF_XML), Ginj.class);
            UIManager.setLookAndFeel(ginjLookAndFeel);
//            UIManager.setLookAndFeel("com.easynth.designer.laf.EaSynthLookAndFeel");
        }
        catch (Exception e) {
            System.err.println("Error loading Ginj look and feel");
            e.printStackTrace();
            System.exit(ERR_STATUS_LAF);
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            new StarWindow().setVisible(true);
        });

    }
}
