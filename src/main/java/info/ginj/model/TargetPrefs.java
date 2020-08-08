package info.ginj.model;

import info.ginj.Ginj;
import info.ginj.export.clipboard.ClipboardExporter;
import info.ginj.export.disk.DiskExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TargetPrefs {

    private static final Logger logger = LoggerFactory.getLogger(TargetPrefs.class);

    // Suffixes (also used in Wizard)
    public static final String TARGET_KEY = "target";
    public static final String EXPORTER_KEY = "exporter";
    public static final String ACCOUNT_KEY = "account";
    public static final String ACCOUNT_USERNAME_KEY = "account.username";
    public static final String ACCOUNT_EMAIL_KEY = "account.email";
    public static final String DISPLAY_NAME_KEY = "display_name";

    // Used to avoid persisting them in settings
    public static final String[] GENERIC_KEYS = new String[] {TARGET_KEY, EXPORTER_KEY, ACCOUNT_KEY, ACCOUNT_USERNAME_KEY, ACCOUNT_EMAIL_KEY, DISPLAY_NAME_KEY};

    List<Target> targetList = new ArrayList<>();

    public static synchronized TargetPrefs load() {
        final File targetPrefsFile = Ginj.getTargetPrefsFile();
        if (targetPrefsFile.exists()) {
            // try to load it
            try (XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(targetPrefsFile)))) {
                return (TargetPrefs) xmlDecoder.readObject();
            }
            catch (Exception e) {
                logger.error("Error loading targets from '" + targetPrefsFile.getAbsolutePath()  + "'. Creating new default targetPrefs.", e);
            }
        }
        return getDefaultTargetPrefs();
    }

    private static TargetPrefs getDefaultTargetPrefs() {
        TargetPrefs targetPrefs = new TargetPrefs();

        // Default disk target

        Target diskTarget = new Target();

        final DiskExporter diskExporter = new DiskExporter();
        diskTarget.setExporter(diskExporter);

        final ExportSettings settings = new ExportSettings();
        settings.setMustAlwaysAskLocation(true);
        settings.setMustRememberLastLocation(true);
        settings.setMustCopyPath(true);
        diskTarget.setSettings(settings);

        diskTarget.setDisplayName(diskExporter.getDefaultShareText());

        targetPrefs.getTargetList().add(diskTarget);


        // Default clipboard target

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
            logger.error("Cannot save targets to '" + targetPrefsFile.getAbsolutePath() + "'. Targets are note saved.", e);
        }
    }

    public List<Target> getTargetList() {
        return targetList;
    }

    public void setTargetList(List<Target> targetList) {
        this.targetList = targetList;
    }
}
