package info.ginj;

/*
TODO Features :
 - Implement fixed ratio to 16:9 (shift-drag) or 4:3 (ctrl-drag) + snap to resp 640x360,800x450,960x540,1280x720 or 320x240,400x300,640x480,800x600,1024x768 / ENHANCEMENT: in 4:3 1280x960
 - Implement Windows detection using JNA
 - Implement video using ffmpeg & Jaffree
 - Implement generic preference editor
 - Finalize history window
 - Should undo/redo change selection inside the Action methods (e.g change color, resize) ? - or completely deselect component after operation
 - Upon export completion, the notification window should allow "auto fade when mouse not over" checkbox + Close button
 - Persist StarWindow position
 - Typing in a Text Overlay should make the overlay grow wider
 - Add Google Drive exporter
 - Add Youtube exporter

TODO UI:
 - Paint title bar
 - Finalize Look and feel (File chooser (save as), Tables)
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

import info.ginj.model.Prefs;
import info.ginj.model.TargetPrefs;
import info.ginj.ui.StarWindow;
import info.ginj.ui.laf.GinjSynthLookAndFeel;
import info.ginj.util.Misc;

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;

public class Ginj {

    public static final String LAF_XML = "/synth.xml";

    public static final int ERR_STATUS_TRANSPARENCY = -1;
    public static final int ERR_STATUS_LAF = -2;
    public static final int ERR_STATUS_LOAD_IMG = -3;
    public static final int ERR_STATUS_OK = 0;

    // caching
    private static final String session = DateTimeFormatter.ofPattern(Misc.DATE_FORMAT_PATTERN).format(LocalDateTime.now());
    private static File tempDir;
    public static FutureTask<JFileChooser> futureFileChooser;
    public static StarWindow starWindow;
    private static TargetPrefs targetPrefs;

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

        javax.swing.SwingUtilities.invokeLater(() -> {
            starWindow = new StarWindow();
            starWindow.setVisible(true);});

    }

    public static String getAppName() {
        return Ginj.class.getSimpleName();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    public static File getHistoryFolder() {
        String historyPath = Prefs.get(Prefs.Key.CAPTURE_HISTORY_PATH);
        if (historyPath == null || historyPath.isBlank() || !new File(historyPath).exists()) {
            historyPath = System.getProperty("user.home") + File.separator + "." + getAppName() + File.separator + "history";
            //noinspection ResultOfMethodCallIgnored
            new File(historyPath).mkdirs();
            Prefs.set(Prefs.Key.CAPTURE_HISTORY_PATH, historyPath);
            Prefs.save();
        }
        return new File(historyPath);
    }

    public static File getPrefsFile() {
        return new File(System.getProperty("user.home") + File.separator + "." + getAppName() + File.separator + "settings.properties");
    }

    public static File getTargetPrefsFile() {
        return new File(System.getProperty("user.home") + File.separator + "." + getAppName() + File.separator + "targetPrefs.xml");
    }

    public static TargetPrefs getTargetPrefs() {
        if (targetPrefs == null) {
            targetPrefs = TargetPrefs.load();
        }
        return targetPrefs;
    }


    public static String getVersion() {
        return "0.2.0";
    }

    public static String getSession() {
        return session;
    }
}
