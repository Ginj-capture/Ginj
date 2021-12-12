package info.ginj.util;

import org.apache.commons.lang3.SystemUtils;

import java.util.Locale;

public class Misc {
    public static final String DATETIME_FORMAT_PATTERN = "yyyy-MM-dd_HH-mm-ss";
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    public static final String IMAGE_FORMAT_PNG = "png";
    public static final String IMAGE_EXTENSION_PNG = ".png";
    public static final String IMAGE_FORMAT_JPEG = "jpeg";
    public static final String IMAGE_EXTENSION_JPEG = ".jpg";
    public static final String VIDEO_EXTENSION = ".mp4";
    public static final String METADATA_EXTENSION = ".xml";
    public static final String THUMBNAIL_EXTENSION = ".thumb.png";

    public static String getPrettySize(double bytes) {
        if (bytes < 1024) return bytes + " B";
        var i = -1;
        do {
            bytes = bytes / 1024;
            i++;
        }
        while (bytes > 1024);
        return String.format(Locale.US, "%.1f", bytes) + UI.SIZE_UNITS[i];
    }

    public static String getPrettySizeRatio(double bytesPartial, double bytesTotal) {
        if (bytesTotal < 1024) return bytesPartial + "/" + bytesTotal + " B";
        var i = -1;
        do {
            bytesTotal = bytesTotal / 1024;
            bytesPartial = bytesPartial / 1024;
            i++;
        }
        while (bytesTotal > 1024);
        return String.format(Locale.US, "%.1f", bytesPartial) + "/" + String.format(Locale.US, "%.1f", bytesTotal) + UI.SIZE_UNITS[i];
    }

    public static boolean isTrue(String property) {
        if (property == null) return false;
        property = property.toLowerCase();
        return property.equals("true") || property.equals("yes") || property.equals("1");
    }

    /**
     * @return "Exit" or "Quit", according to the OS.
     */
    public static String getExitQuitText() {
        if (SystemUtils.IS_OS_WINDOWS) return "Exit";
        else return "Quit";
    }
}
