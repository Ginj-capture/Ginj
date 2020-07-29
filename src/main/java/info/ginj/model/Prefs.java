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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Prefs {

    private static final Logger logger = LoggerFactory.getLogger(Prefs.class);

    private static final String DEFAULT_TOOL_LIST = ArrowTool.NAME + "," + TextTool.NAME + "," + FrameTool.NAME + "," + HighlightTool.NAME;

    public enum Key {
        TOOL_COLOR_PREFIX("tool.color.", "The current color for the corresponding tool", false),
        FIXED_PALETTE_COLOR_PREFIX("fixed.palette.color.", "The color for the corresponding button in the fixed palette", true),
        TOOL_LIST("tool.list", "The list of active overlays", false),
        CAPTURE_HISTORY_PATH("capture.history.path", "The folder where all capture history is stored" , true),
        USE_SMALL_BUTTONS_FOR_ONLINE_TARGETS("use.small.buttons.for.online.target", "If set, small buttons like are shown for online targets, like for save and copy", true),
        EXPORT_COMPLETE_AUTOHIDE_KEY("export.complete.autohide", "If set, the window displayed upon export completion will fade away and close when not hovered", true),
        STAR_WINDOW_POSTION_ON_BORDER("star.window.position.on.border", "This indicates the screen border that the 'Star' icon is resting on", true),
        STAR_WINDOW_DISTANCE_FROM_CORNER("star.window.distance.from.corner", "This indicates the distance from the top or left edge of the screen to the 'Star' icon", true),
        STAR_WINDOW_DISPLAY_NUMBER("star.window.display.number", "This is the number of the display the 'Star' icon should be displayed on (0=Main, 1=Secondary, ...)", true),
        CAPTURE_HOTKEY("capture.hotkey", "This is the combination to type to trigger a new capture", false),
        USE_TRAY_NOTIFICATION_ON_EXPORT_COMPLETION("use.tray.notification.on.export.completion", "If enabled, the 'end of export' window is replaced by an OS tray notification" , true);

        private final String keyString;
        private final String help;
        private final boolean isEditable;

        Key(String keyString, String help, boolean isEditable) {
            this.keyString = keyString;
            this.help = help;
            this.isEditable = isEditable;
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
    }


    private static final Properties preferences = new Properties();

    static void resetToDefaults() {
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "0", new Color(0,0,0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "1", new Color(255,255,255));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "2", new Color(255,0,0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "3", new Color(255,165,0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "4", new Color(255,255,0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "5", new Color(0,128,0));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "6", new Color(0,0,255));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "7", new Color(128,0,128));
        setColorWithSuffix(Key.FIXED_PALETTE_COLOR_PREFIX, "8", new Color(75,0,130));
        set(Key.TOOL_LIST, DEFAULT_TOOL_LIST);
    }


    public static void load() {
        File preferencesFile = Ginj.getPrefsFile();

        try (final FileReader reader = new FileReader(preferencesFile)) {
            preferences.load(reader);
        }
        catch (IOException e) {
            logger.error("Error reading preferences from " + preferencesFile.getAbsolutePath() + ". Using default preferences...", e);
            resetToDefaults();
        }
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
        return preferences.getProperty(key.keyString);
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
    public static void set(Key key, String value) {
        preferences.setProperty(key.keyString, value);
    }

    public static void setColor(Key key, Color color) {
        set(key, UI.colorToHex(color));
    }

    public static void remove(Key key) {
        preferences.remove(key);
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

    public static List<GinjTool> getToolList() {
        List<GinjTool> toolList = new ArrayList();
        String toolListStr = Prefs.get(Key.TOOL_LIST);
        if (toolListStr == null || toolListStr.length() == 0) {
            toolListStr = DEFAULT_TOOL_LIST;
        }
        String[] toolNames = toolListStr.split(",");
        for (String toolName : toolNames) {
            toolList.add(GinjTool.getMap().get(toolName));
        }
        return toolList;
    }
}
