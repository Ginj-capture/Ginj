package info.ginj;

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
        return "0.3.3";
    }

    public static String getSession() {
        return session;
    }
}
