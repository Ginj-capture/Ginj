package info.ginj;

import info.ginj.model.Prefs;
import info.ginj.model.TargetPrefs;
import info.ginj.ui.StarWindow;
import info.ginj.ui.laf.GinjSynthLookAndFeel;
import info.ginj.util.Jaffree;
import info.ginj.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;

public class Ginj {

    private static final Logger logger = LoggerFactory.getLogger(Ginj.class);

    public static final String APP_VERSION = "0.4.9";

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

    static {
        try {
            LogManager.getLogManager().readConfiguration(Ginj.class.getResourceAsStream("/logging.properties"));
        }
        catch (final Exception e) {
            System.err.println("Error! - Could not load default logging.properties file");
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Prefs.load();

        // Init logging level
        initLoggingLevel();
        logger.info(getAppName() + " " + getVersion() + " starting.");

        // Determine what the GraphicsDevice can support.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        //If translucent windows aren't supported, exit.
        if (!gd.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)) {
            logger.error("Per-pixel translucency is not supported");
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
            logger.error("Error loading " + getAppName() + " look and feel", e);
            System.exit(ERR_STATUS_LAF);
        }

        // Check ffpmeg availability
        Jaffree.checkAvailability();

        // Creating a JFileChooser can take time if you have network drives. So start loading one now, in a separate thread...
        // TODO check if this is really effective...
        futureFileChooser = new FutureTask<>(JFileChooser::new);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(futureFileChooser);

        javax.swing.SwingUtilities.invokeLater(() -> {
            starWindow = new StarWindow();
            starWindow.setVisible(true);
        });

    }

    private static void initLoggingLevel() {
        String levelStr = Prefs.get(Prefs.Key.LOGGING_LEVEL);
        if (levelStr == null) {
            levelStr = Level.INFO.getName();
            Prefs.set(Prefs.Key.LOGGING_LEVEL, levelStr);
        }
        setLoggingLevel(levelStr);
    }

    public static void setLoggingLevel(String levelStr) {
        // This assumes that slf4j uses the JDK logging framework
        Level level;
        try {
            level = Level.parse(levelStr);
        }
        catch (IllegalArgumentException e) {
            level = Level.INFO;
            Prefs.set(Prefs.Key.LOGGING_LEVEL, level.getName());
        }
        for (Handler handler : java.util.logging.Logger.getLogger("").getHandlers()) {
            handler.setLevel(level);
        }
    }

    public static String getAppName() {
        return Ginj.class.getSimpleName();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getTempDir() {
        if (tempDir == null) {
            // First invocation, check, clean or create temp dir
            String tempDirName = Prefs.get(Prefs.Key.TEMP_DIR);
            if (tempDirName != null) {
                tempDir = new File(tempDirName);
                if (!tempDir.exists()) {
                    if (!tempDir.mkdirs()) {
                        logger.error(tempDirName + " declared as " + Prefs.Key.TEMP_DIR.getKey() + " option cannot be found nor created as a directory. Using a subdir of the system temp dir.");
                        tempDir = null;
                    }
                }
                else {
                    if (!tempDir.isDirectory()) {
                        logger.error(tempDirName + " declared as " + Prefs.Key.TEMP_DIR.getKey() + " option is not a directory. Using a subdir of the system temp dir.");
                        tempDir = null;
                    }
                }
            }
            if (tempDir == null) {
                tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + getAppName() + "_temp");
                if (tempDir.exists()) {
                    // Cleanup
                    for (File file : Ginj.tempDir.listFiles()) {
                        file.delete();
                    }
                }
                else {
                    // Create
                    tempDir.mkdirs();
                }
            }
        }
        // And return it
        return tempDir;
    }

    private static File getAppFolder() {
        File folder = new File(System.getProperty("user.home") + File.separator + "." + getAppName());
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                logger.error("Could not create app folder ." + Ginj.getAppName());
            }
        }
        return folder;
    }

    public static File getHistoryFolder() {
        String historyPath = Prefs.get(Prefs.Key.CAPTURE_HISTORY_PATH);
        if (historyPath == null || historyPath.isBlank() || !new File(historyPath).exists()) {
            File historyFolder = new File(getAppFolder(), "history");
            historyFolder.mkdirs();
            historyPath = historyFolder.getAbsolutePath();
            Prefs.set(Prefs.Key.CAPTURE_HISTORY_PATH, historyPath);
            Prefs.save();
        }
        return new File(historyPath);
    }

    public static File getPrefsFile() {
        return new File(getAppFolder(), "settings.properties");
    }

    public static File getTargetPrefsFile() {
        return new File(getAppFolder(), "targetPrefs.xml");
    }

    public static TargetPrefs getTargetPrefs() {
        if (targetPrefs == null) {
            targetPrefs = TargetPrefs.load();
        }
        return targetPrefs;
    }


    public static String getVersion() {
        return APP_VERSION;
    }

    public static String getSession() {
        return session;
    }
}
