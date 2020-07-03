package info.ginj.model;

import info.ginj.export.GinjExporter;
import info.ginj.export.clipboard.ClipboardExporterImpl;
import info.ginj.export.disk.DiskExporterImpl;
import info.ginj.export.online.dropbox.DropboxExporter;
import info.ginj.export.online.google.GooglePhotosExporter;
import info.ginj.util.Util;

import javax.swing.*;

public enum ExportTarget {
    DROPBOX("Add to Dropbox", "/img/logo/dropbox.png", true, DropboxExporter.class),
    GOOGLEPHOTOS("Add to Google Photos", "/img/logo/googlephotos.png", true, GooglePhotosExporter.class),
    DISK("Save", "/img/icon/save.png", false, DiskExporterImpl.class),
    CLIPBOARD("Copy", "/img/icon/copy.png", false, ClipboardExporterImpl.class),
    ;

    private final String help;
    private final String iconPath;
    private final boolean isOnlineService;
    private final Class<? extends GinjExporter> exporterClass;

    ExportTarget(String help, String iconPath, boolean isOnlineService, Class<? extends GinjExporter> exporterClass) {
        this.help = help;
        this.iconPath = iconPath;
        this.isOnlineService = isOnlineService;
        this.exporterClass = exporterClass;
    }

    public String getHelp() {
        return help;
    }

    public String getIconPath() {
        return iconPath;
    }

    public boolean isOnlineService() {
        return isOnlineService;
    }

    public Class<? extends GinjExporter> getExporterClass() {
        return exporterClass;
    }

    // Helper

    public ImageIcon getButtonIcon(int size) {
        if (isOnlineService) {
            // Use official logo and don't colorize
            return Util.createIcon(getClass().getResource(getIconPath()), size, size);
        }
        else {
            // Colorize
            return Util.createIcon(getClass().getResource(getIconPath()), size, size, Util.ICON_ENABLED_COLOR);
        }
    }

}
