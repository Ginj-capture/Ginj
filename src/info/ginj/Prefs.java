package info.ginj;

import info.ginj.ui.Util;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Prefs {

    // Needed to avoid circular references
    private static final String SAVE_LOCATION_DIR_KEY_NAME = "save.location.dir";

    public enum Key {
        USE_CUSTOM_LOCATION("use.custom.location", "If set, user must select target folder and filename each time he saves a capture. Otherwise the file is saved with the proposed name, in the folder defined under '" + SAVE_LOCATION_DIR_KEY_NAME + "'.", true),
        SAVE_LOCATION_DIR(SAVE_LOCATION_DIR_KEY_NAME, "If '" + USE_CUSTOM_LOCATION.keyString + "' is false, folder to save captures in.", true),
        DEFAULT_CUSTOM_SAVE_LOCATION_DIR("default.custom.save.location.dir", "If '" + USE_CUSTOM_LOCATION.keyString + "' is true, default folder to propose to save captures in. If empty, the last folder used will be proposed.", true),
        LAST_CUSTOM_SAVE_LOCATION_DIR("last.custom.save.location.dir", "If '" + USE_CUSTOM_LOCATION.keyString + "' is true and '" + DEFAULT_CUSTOM_SAVE_LOCATION_DIR.keyString + "' is empty, this folder is proposed", false),

        TOOL_COLOR_PREFIX("tool.color.", "The current color for the corresponding tool", false),
        FIXED_PALETTE_COLOR_PREFIX("fixed.palette.color.", "The color for the corresponding button in the fixed palette", true),

        EXPORTER_DROPBOX_USERNAME_PREFIX("exporter.dropbox.username.", "The username of the linked Dropbox account", false),
        EXPORTER_DROPBOX_CREATE_LINK_PREFIX("exporter.dropbox.create.link.", "If true, a public shared link is created and copied to the clipboard after upload", true),

        EXPORTER_DROPBOX_ACCESS_TOKEN_PREFIX("exporter.dropbox.access.token.", "The last token received to access with Dropbox", false),
        EXPORTER_DROPBOX_ACCESS_EXPIRY_PREFIX("exporter.dropbox.access.expiry.", "The expiry time of the token used to access Dropbox", false),
        EXPORTER_DROPBOX_REFRESH_TOKEN_PREFIX("exporter.dropbox.refresh.token.", "The token to use to request a new access token to Dropbox", false),

        EXPORTER_GOOGLE_PHOTOS_ACCESS_TOKEN_PREFIX("exporter.googlephotos.access.token.", "The last token received to access with Google Photos", false),
        EXPORTER_GOOGLE_PHOTOS_ACCESS_EXPIRY_PREFIX("exporter.googlephotos.access.expiry.", "The expiry time of the token used to access Google Photos", false),
        EXPORTER_GOOGLE_PHOTOS_REFRESH_TOKEN_PREFIX("exporter.googlephotos.refresh.token.", "The token to use to request a new access token to Google Photos", false),

        EXPORTER_GOOGLE_DRIVE_ACCESS_TOKEN_PREFIX("exporter.googledrive.access.token.", "The last token received to access with Google Drive", false),
        EXPORTER_GOOGLE_DRIVE_ACCESS_EXPIRY_PREFIX("exporter.googledrive.access.expiry.", "The expiry time of the token used to access Google Drive", false),
        EXPORTER_GOOGLE_DRIVE_REFRESH_TOKEN_PREFIX("exporter.googledrive.refresh.token.", "The token to use to request a new access token to Google Drive", false),

        EXPORTER_YOUTUBE_ACCESS_TOKEN_PREFIX("exporter.youtube.access.token.", "The last token received to access with Youtube", false),
        EXPORTER_YOUTUBE_ACCESS_EXPIRY_PREFIX("exporter.youtube.access.expiry.", "The expiry time of the token used to access Youtube", false),
        EXPORTER_YOUTUBE_REFRESH_TOKEN_PREFIX("exporter.youtube.refresh.token.", "The token to use to request a new access token to Youtube", false),

        EXPORTER_GOOGLE_PHOTOS_ALBUM_GRANULARITY("exporter.googlephotos.album.granularity.", "One Google Photo album will be created by... (APP, DAY, SESSION, NAME, CAPTURE)" , true);

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
    }

    static File getFile() {
        // TODO where does this file end up ? Is it guaranteed ?
        return new File(Ginj.getAppName() + ".properties");
    }


    public static void load() {
        File preferencesFile = getFile();

        try (final FileReader reader = new FileReader(preferencesFile)) {
            preferences.load(reader);
        }
        catch (IOException e) {
            System.err.println("Error reading preferences from " + preferencesFile.getAbsolutePath() + ". Using default preferences...");
            resetToDefaults();
        }
    }

    public static void save() {
        File preferencesFile = getFile();

        if (preferencesFile.exists()) {
            // Backup
            File backupFile = new File(getFile().getAbsolutePath() + ".bak");
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
        return Ginj.isTrue(get(key));
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
        set(key, Util.colorToHex(color));
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
        return Ginj.isTrue(getWithSuffix(key, suffix));
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
        setWithSuffix(key, suffix, Util.colorToHex(color));
    }

    public static void removeWithSuffix(Key key, String suffix) {
        preferences.remove(key.keyString + suffix);
    }

}
