package info.ginj.model;

import info.ginj.Ginj;
import info.ginj.tool.GinjTool;
import info.ginj.tool.arrow.ArrowTool;
import info.ginj.tool.frame.FrameTool;
import info.ginj.tool.highlight.HighlightTool;
import info.ginj.tool.text.TextTool;
import info.ginj.util.Misc;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class Prefs {

    private static final Logger logger = LoggerFactory.getLogger(Prefs.class);

    private static Set<GinjTool> toolSet;

    public enum Key {
        TOOL_COLOR_PREFIX("tool.color.", "The current color for the corresponding tool", false),
        FIXED_PALETTE_COLOR_PREFIX("fixed.palette.color.", "The color for the corresponding button in the fixed palette", true),
        TOOL_LIST("tool.list", "The list of active overlays", false, ArrowTool.NAME + "," + TextTool.NAME + "," + FrameTool.NAME + "," + HighlightTool.NAME),
        CAPTURE_HISTORY_PATH("capture.history.path", "The folder where all capture history is stored", true),
        USE_SMALL_BUTTONS_FOR_ONLINE_TARGETS("use.small.buttons.for.online.target", "If set, small buttons like are shown for online targets, like for save and copy", true, String.valueOf(false)),
        EXPORT_COMPLETE_AUTOHIDE_KEY("export.complete.autohide", "If set, the window displayed upon export completion will fade away and close when not hovered", true, String.valueOf(false)),
        STAR_WINDOW_POSTION_ON_BORDER("star.window.position.on.border", "This indicates the screen border that the 'Star' icon is resting on", true),
        @Deprecated
        STAR_WINDOW_DISTANCE_FROM_CORNER("star.window.distance.from.corner", "This indicates the distance from the top or left edge of the screen to the 'Star' icon", true),
        STAR_WINDOW_DISTANCE_FROM_CORNER_PERCENT("star.window.distance.from.corner.percent", "This indicates the distance from the top or left edge of the screen to the 'Star' icon", true, String.valueOf(50)),
        STAR_WINDOW_DISPLAY_NUMBER("star.window.display.number", "This is the number of the display the 'Star' icon should be displayed on (0=Main, 1=Secondary, ...)", true, String.valueOf(0)),
        CAPTURE_HOTKEY("capture.hotkey", "This is the combination to type to trigger a new capture", false),
        FFMPEG_BIN_DIR("ffmpeg.bin.dir", "Folder where the ffmpeg binary executable can be founs", true),
        VIDEO_FRAMERATE("video.framerate", "The framerate of the video captures", true, String.valueOf(10)),
        VIDEO_CAPTURE_MOUSE_CURSOR("video.capture.mouse.cursor", "If true, the mouse cursor is captured in the video", true),
        VIDEO_IMAGE_UPDATE_DELAY_MS("video.image.update.delay.ms", "Delay before update of the image when dragging the slider on the timeline of a video capture", true, String.valueOf(150)),
        USE_TRAY_NOTIFICATION_ON_EXPORT_COMPLETION("use.tray.notification.on.export.completion", "If enabled, the 'end of export' window is replaced by an OS tray notification", true, String.valueOf(true)),
        TEMP_DIR("temp.dir", "The directory to store temporary captures", true),
        DEBUG_NO_OPACITY_CHANGE("debug.no.opacity.change", "Debug param to try to avoid the transparent grey background", true),
        DEBUG_NO_FAKE_TRANSPARENCY("debug.no.fake.transparency", "Debug param to avoid the slight opacity of the deployed widget", true),
        DEBUG_NO_SETVISIBLE_FALSE_IN_RECOVERY("debug.no.setvisible.false.in.recovery", "Debug param to see what part really recovers the widget", true),
        DEBUG_NO_SETVISIBLE_TRUE_IN_RECOVERY("debug.no.setvisible.true.in.recovery", "Debug param to see what part really recovers the widget", true),
        DEBUG_NO_TO_FRONT_IN_RECOVERY("debug.no.to.front.in.recovery", "Debug param to see what part really recovers the widget", true),
        DEBUG_NO_REQUEST_FOCUS_IN_RECOVERY("debug.no.request.focus.in.recovery", "Debug param to see what part really recovers the widget", true),
        FFMPEG_TERMINATION_TIMEOUT("ffmpeg.termination.timeout", "The max delay between a request to end an ffmpeg process and its actual response", true, String.valueOf(15)),
        USE_JNA_FOR_WINDOWS_MONITORS("use.jna.for.windows.monitors", "If true, dimensions of monitors and mouse position will be fetched using JNA to work around bug JDK-8211999", true, String.valueOf(true)),
        LOGGING_LEVEL("logging.level", "The verbosity of the log file. Valid values are OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST and ALL", true),
        DEFAULT_TOOL_NAME("default.tool.name", "The tool that is selected by default after a new capture is performed" , true),
        REMEMBER_DEFAULT_TOOL("remember.default.tool", "Remember the default tool according to the first tool used in the previous capture" , true),
        HISTORY_WINDOW_WIDTH("history.window.width", "Width of the history window", false),
        HISTORY_WINDOW_HEIGHT("history.window.height", "Height of the history window", false);

        private final String keyString;
        private final String help;
        private final boolean isEditable;
        private final String defaultValue;

        Key(String keyString, String help, boolean isEditable) {
            this.keyString = keyString;
            this.help = help;
            this.isEditable = isEditable;
            defaultValue = null;
        }

        Key(String keyString, String help, boolean isEditable, String defaultValue) {
            this.keyString = keyString;
            this.help = help;
            this.isEditable = isEditable;
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return keyString;
        }

        public String getHelp() {
            return help;
        }

        public boolean isEditable() {
            return isEditable;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }


    private static final Properties preferences = new Properties();

    static void resetToDefaults() {
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "0", new Color(0, 0, 0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "1", new Color(255, 255, 255));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "2", new Color(255, 0, 0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "3", new Color(255, 165, 0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "4", new Color(255, 255, 0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "5", new Color(0, 128, 0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "6", new Color(0, 0, 255));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "7", new Color(128, 0, 128));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "8", new Color(75, 0, 130));
        set(Key.TOOL_LIST, Key.TOOL_LIST.getDefaultValue());
    }


    public static void load() {
        File preferencesFile = Ginj.getPrefsFile();
        if (preferencesFile.exists()) {
            // try to load it
            try (final FileReader reader = new FileReader(preferencesFile)) {
                preferences.load(reader);
                return;
            }
            catch (IOException e) {
                logger.error("Error reading preferences from " + preferencesFile.getAbsolutePath() + ". Using default preferences...", e);
            }
        }
        resetToDefaults();
    }

    public static void save() {
        File preferencesFile = Ginj.getPrefsFile();

        if (preferencesFile.exists()) {
            // Backup
            File backupFile = new File(Ginj.getPrefsFile().getAbsolutePath() + ".bak");
            if (backupFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                backupFile.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            preferencesFile.renameTo(backupFile);
        }
        try (final FileWriter writer = new FileWriter(preferencesFile)) {
            preferences.store(writer, Ginj.getAppName() + " preferences");
        }
        catch (IOException e) {
            logger.error("Error writing preferences to " + preferencesFile.getAbsolutePath() + ". Preferences are not saved...", e);
        }
    }


    public static String get(Key key) {
        String property = preferences.getProperty(key.keyString);
        if (property == null || property.length() == 0) {
            property = key.defaultValue;
        }
        return property;

    }

    public static String get(Key key, String defaultValue) {
        return preferences.getProperty(key.keyString, defaultValue);
    }

    public static boolean isTrue(Key key) {
        return Misc.isTrue(get(key));
    }

    public static Color getColor(Key key) {
        final String hexColor = get(key);
        if (hexColor == null || hexColor.isBlank()) return null;
        return Color.decode(hexColor);
    }

    public static long getAsLong(Key key, long defaultValue) {
        String valueStr = get(key);
        if (valueStr == null || valueStr.length() == 0) {
            return defaultValue;
        }
        else {
            try {
                return Long.parseLong(valueStr);
            }
            catch (NumberFormatException e) {
                logger.error("Value '" + valueStr + "' cannot be converted into a number for key '" + key.getKey() + "'.", e);
                return defaultValue;
            }
        }
    }

    public static long getAsLong(Key key) {
        String valueStr = get(key);
        if (valueStr == null || valueStr.length() == 0) {
            return Long.parseLong(key.getDefaultValue());
        }
        else {
            try {
                return Long.parseLong(valueStr);
            }
            catch (NumberFormatException e) {
                logger.error("Value '" + valueStr + "' cannot be converted into a number for key '" + key.getKey() + "'.", e);
                return Long.parseLong(key.getDefaultValue());
            }
        }
    }

    public static long getAsInt(Key key, int defaultValue) {
        String valueStr = get(key);
        if (valueStr == null || valueStr.length() == 0) {
            return defaultValue;
        }
        else {
            try {
                return Integer.parseInt(valueStr);
            }
            catch (NumberFormatException e) {
                logger.error("Value '" + valueStr + "' cannot be converted into a number for key '" + key.getKey() + "'.", e);
                return defaultValue;
            }
        }
    }

    public static int getAsInt(Key key) {
        String valueStr = get(key);
        if (valueStr == null || valueStr.length() == 0) {
            return Integer.parseInt(key.defaultValue);
        }
        else {
            try {
                return Integer.parseInt(valueStr);
            }
            catch (NumberFormatException e) {
                logger.error("Value '" + valueStr + "' cannot be converted into a number for key '" + key.getKey() + "'.", e);
                return Integer.parseInt(key.defaultValue);
            }
        }
    }

    public static void set(Key key, String value) {
        preferences.setProperty(key.keyString, value);
    }

    public static void setColor(Key key, Color color) {
        set(key, UI.colorToHex(color));
    }

    public static void remove(Key key) {
        preferences.remove(key.keyString);
    }


    public static String getWithSuffix(Key key, String suffix) {
        return preferences.getProperty(key.keyString + suffix);
    }

    public static String getWithSuffix(Key key, String suffix, String defaultValue) {
        return preferences.getProperty(key.keyString + suffix, defaultValue);
    }

    public static boolean isTrueWithSuffix(Key key, String suffix) {
        return Misc.isTrue(getWithSuffix(key, suffix));
    }

    public static Color getColorWithSuffix(Key key, String suffix) {
        final String hexColor = getWithSuffix(key, suffix);
        if (hexColor == null || hexColor.isBlank()) return null;
        try {
            return Color.decode(hexColor);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static void setWithSuffix(Key key, String suffix, String value) {
        preferences.setProperty(key.keyString + suffix, value);
    }

    public static void setColorWithSuffix(Key key, String suffix, Color color) {
        setWithSuffix(key, suffix, UI.colorToHex(color));
    }

    public static void removeWithSuffix(Key key, String suffix) {
        preferences.remove(key.keyString + suffix);
    }

    public static Set<GinjTool> getToolSet() {
        if (toolSet == null) {
            toolSet = new LinkedHashSet<>();
            String toolListStr = Prefs.get(Key.TOOL_LIST);
            String[] toolNames = toolListStr.split(",");
            for (String toolName : toolNames) {
                toolSet.add(GinjTool.getMap().get(toolName));
            }
        }
        return toolSet;
    }

    @SuppressWarnings("StringConcatenationInLoop")
    public static void setToolSet(Set<GinjTool> toolSet) {
        Prefs.toolSet = toolSet;
        String toolNames = "";
        for (GinjTool ginjTool : toolSet) {
            if (toolNames.length() > 0) {
                toolNames += ",";
            }
            toolNames += ginjTool.getName();
        }
        Prefs.set(Key.TOOL_LIST, toolNames);
    }
}
