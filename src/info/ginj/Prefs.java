package info.ginj;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Prefs {

    // Needed to avoid circular references
    private static final String SAVE_LOCATION_DIR_KEY_NAME = "save.location.dir";

    public static enum Key {
        USE_CUSTOM_LOCATION("use.custom.location", "If set, user must select target folder and filename each time he saves a capture. Otherwise the file is saved with the proposed name, in the folder defined under '" + SAVE_LOCATION_DIR_KEY_NAME + "'.", true),
        SAVE_LOCATION_DIR(SAVE_LOCATION_DIR_KEY_NAME, "If '" + USE_CUSTOM_LOCATION.keyString + "' is false, folder to save captures in.", true),
        DEFAULT_CUSTOM_SAVE_LOCATION_DIR("default.custom.save.location.dir", "If '" + USE_CUSTOM_LOCATION.keyString + "' is true, default folder to propose to save captures in. If empty, the last folder used will be proposed.", true),
        LAST_CUSTOM_SAVE_LOCATION_DIR("last.custom.save.location.dir", "If '" + USE_CUSTOM_LOCATION.keyString + "' is true and '" + DEFAULT_CUSTOM_SAVE_LOCATION_DIR.keyString +"' is empty, this folder is proposed", false);

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

    private static Properties preferences = new Properties();

    static File getFileName() {
        // TODO where does this file end up ? Is it guaranteed ?
        return new File(Ginj.getAppName() + ".properties");
    }

    public static void load() {
        File preferencesFile = getFileName();

        try (final FileReader reader = new FileReader(preferencesFile)) {
            preferences.load(reader);
        }
        catch (IOException e) {
            System.err.println("Error reading preferences from " + preferencesFile.getAbsolutePath() + ". Using blank preferences...");
        }
    }

    public static void save() {
        File preferencesFile = getFileName();

        try (final FileWriter writer = new FileWriter(preferencesFile)) {
            preferences.store(writer, Ginj.getAppName() + " preferences");
        }
        catch (IOException e) {
            System.out.println("Error writing preferences to " + preferencesFile.getAbsolutePath() + ". Preferences are not saved...");
        }
    }

    public static String get(Key key) {
        return preferences.getProperty(key.keyString);
    }

    public static String get(Key key, String defaultValue) {
        return preferences.getProperty(key.keyString, defaultValue);
    }

    public static boolean isTrue(Key key) {
        return Ginj.isTrue(preferences.getProperty(key.keyString));
    }

    public static void set(Key key, String value) {
        preferences.setProperty(key.keyString, value);
    }
}
