package info.ginj.model;

import info.ginj.Ginj;
import info.ginj.export.clipboard.ClipboardExporter;
import info.ginj.export.disk.DiskExporter;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TargetPrefs {

    // Suffixes (also used in Wizard)
    public static final String EXPORTER_KEY = "exporter";
    public static final String ACCOUNT_KEY = "account";
    public static final String ACCOUNT_USERNAME_KEY = "account.username";
    public static final String ACCOUNT_EMAIL_KEY = "account.email";
    public static final String DISPLAY_NAME_KEY = "display_name";
    public static final String ALBUM_GRANULARITY_KEY = "album_granularity";
    public static final String MUST_SHARE_KEY = "must_share";
    public static final String MUST_COPY_PATH_KEY = "must_copy_path";
    public static final String DEST_LOCATION_KEY = "save_location";
    public static final String LAST_CUSTOM_DEST_LOCATION_KEY = "last_custom_save_location";
    public static final String ALWAYS_ASK_DIR_KEY = "always_ask_dir";
    public static final String REMEMBER_DIR_KEY = "remember_dir";


    List<Target> targetList = new ArrayList<>();

    public static synchronized TargetPrefs load() {
        final File targetPrefsFile = Ginj.getTargetPrefsFile();
        try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(targetPrefsFile)))) {
            return (TargetPrefs) xmlDecoder.readObject();
        }
        catch (Exception e) {
            System.err.println("Error loading targets from '" + targetPrefsFile.getAbsolutePath()  + "'. Creating new default targetPrefs.");
            return getDefaultTargetPrefs();
        }
    }

    private static TargetPrefs getDefaultTargetPrefs() {
        TargetPrefs targetPrefs = new TargetPrefs();

        Target diskTarget = new Target();
        final DiskExporter diskExporter = new DiskExporter();
        diskTarget.setExporter(diskExporter);
        diskTarget.setDisplayName(diskExporter.getDefaultShareText());
        diskTarget.getOptions().put(TargetPrefs.ALWAYS_ASK_DIR_KEY, "true");
        diskTarget.getOptions().put(TargetPrefs.REMEMBER_DIR_KEY, "true");
        diskTarget.getOptions().put(TargetPrefs.MUST_COPY_PATH_KEY, "true");
        targetPrefs.getTargetList().add(diskTarget);

        Target clipboardTarget = new Target();
        final ClipboardExporter clipboardExporter = new ClipboardExporter();
        clipboardTarget.setExporter(clipboardExporter);
        clipboardTarget.setDisplayName(clipboardExporter.getDefaultShareText());
        targetPrefs.getTargetList().add(clipboardTarget);

        return targetPrefs;
    }

    public synchronized void save() {
        File targetPrefsFile = Ginj.getTargetPrefsFile();

        if (targetPrefsFile.exists()) {
            // Backup
            File backupFile = new File(Ginj.getTargetPrefsFile().getAbsolutePath() + ".bak");
            if (backupFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                backupFile.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            targetPrefsFile.renameTo(backupFile);
        }
        try (XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(targetPrefsFile)))) {
            xmlEncoder.writeObject(this);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot save targets to '" + targetPrefsFile.getAbsolutePath() + "'. Targets are note saved.");
        }
    }

    public List<Target> getTargetList() {
        return targetList;
    }

    public void setTargetList(List<Target> targetList) {
        this.targetList = targetList;
    }
}
