package info.ginj;

/*
TODO Features :
 - Implement fixed ratio to 16:9 (shift-drag) or 4:3 (ctrl-drag) + snap to resp 640x360,800x450,960x540,1280x720 or 320x240,400x300,640x480,800x600,1024x768 / ENHANCEMENT: in 4:3 1280x960
 - Implement Windows detection using JNA
 - Implement "share" export
 - Implement video using ffmpeg & Jaffree
 - Implement history
 - Implement preference editor
 - Should undo/redo change selection inside the Action methods (e.g change color, resize) ? - or completely deselect component after operation
 - Exports should be made in a separate Dialog (with progress + and notification when done, + "auto fade when mouse not over" checkbox + Close button, with return to the main window in case of error
 - Persist StarWindow position

TODO UI:
 - Paint title bar
 - Finalize Look and feel (File chooser (save as), Color chooser (radio buttons and cursors), Tables)
 - Fix scrollbar corner + thumb icon + colors + gap
 - Upscale sun and sun-rays so that runtime downscale provides an anti-aliasing, or better yet draw it by code (gradients etc)
 - Build 3 main buttons at runtime based on circle + icons (downscale provides an anti-aliasing)

TODO Cleanup:
 - Remove EASynth resource dir
 - Remove useless EASynth classes, if any
 - Remove useless icons ?

TODO Options ENHANCEMENT:
 - Improve color chooser UI
 - Add optional "Speech Balloon" overlay
 - Add optional "Line" overlay (with CTRL to constrain)
 - Shift should constrain handle move horizontally/vertically, Ctrl should resize symmetrically
 - Add overlays on video

*/

import info.ginj.ui.laf.GinjSynthLookAndFeel;

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;

public class Ginj {

    public static final String LAF_XML = "laf.xml";

    public static final int ERR_STATUS_TRANSPARENCY = -1;
    public static final int ERR_STATUS_LAF = -2;
    public static final int ERR_STATUS_LOAD_IMG = -3;
    public static final int ERR_STATUS_OK = 0;

    // caching
    public static FutureTask<JFileChooser> futureFileChooser;
    private static File tempDir;

    public static void main(String[] args) {
        // Determine what the GraphicsDevice can support.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        //If translucent windows aren't supported, exit.
        if (!gd.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)) {
            System.err.println("Per-pixel translucency is not supported");
            System.exit(ERR_STATUS_TRANSPARENCY);
        }

        SynthLookAndFeel ginjLookAndFeel = new GinjSynthLookAndFeel();
        try {
            ginjLookAndFeel.load(Ginj.class.getResourceAsStream(LAF_XML), Ginj.class);
            UIManager.setLookAndFeel(ginjLookAndFeel);
//            UIManager.setLookAndFeel(EaSynthLookAndFeel.class.getName());
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            System.err.println("Error loading Ginj look and feel");
            e.printStackTrace();
            System.exit(ERR_STATUS_LAF);
        }

        Prefs.load();

        // Creating a JFileChooser can take time if you have network drives. So start loading one now, in a separate thread...
        // TODO check if this is really effective...
        futureFileChooser = new FutureTask<>(JFileChooser::new);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(futureFileChooser);

        javax.swing.SwingUtilities.invokeLater(() -> new StarWindow().setVisible(true));

    }

    public static boolean isTrue(String property) {
        if (property == null) return false;
        property = property.toLowerCase();
        return property.equals("true") || property.equals("yes") || property.equals("1");
    }

    public static String getAppName() {
        return Ginj.class.getSimpleName();
    }

    public static File getTempDir() {
        if (tempDir == null) {
            // First invocation, check, clean or create temp dir
            tempDir = new File(System.getProperty("java.io.tmpdir") + getAppName() + "_temp");
            if (tempDir.exists()) {
                // Cleanup
                for (File file : tempDir.listFiles()) {
                    file.delete();
                }
            }
            else {
                // Create
                tempDir.mkdirs();
            }
        }
        // And return it
        return tempDir;
    }

    public static String getVersion() {
        return "0.1";
    }
}
