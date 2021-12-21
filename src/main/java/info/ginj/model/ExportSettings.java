package info.ginj.model;


// TODO we should probably have subclasses of ExportSettings per Exporter. Having Google Photos related info here is ugly
import info.ginj.export.online.google.GooglePhotosExporter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExportSettings {

    public enum FileFormat {
        PNG("PNG format"),
        JPEG("JPEG format"),
        SELECT_ON_SAVE("Select format on save"),
        SMALLEST("Whichever makes the smallest file");

        private final String friendlyName;

        FileFormat(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        @Override
        public String toString() {
            return friendlyName;
        }
    }

    // Warning: all these fields must be handled by the copyToMap() & moveFromMap() methods
    public static final String MUST_ALWAYS_ASK_LOCATION_KEY = "must_always_ask_location";
    public static final String DEST_LOCATION_KEY = "dest_location";
    public static final String MUST_REMEMBER_LAST_LOCATION_KEY = "must_remember_last_location";
    public static final String PREFERRED_FILE_FORMAT_KEY = "preferred_file_format";
    public static final String MUST_SHARE_KEY = "must_share";
    public static final String MUST_COPY_PATH_KEY = "must_copy_path";
    public static final String ALBUM_GRANULARITY_KEY = "album_granularity";

    private Boolean mustAlwaysAskLocation;
    private String destLocation;
    private Boolean mustRememberLastLocation;
    private FileFormat preferredFileFormat = FileFormat.PNG;
    private Boolean mustShare;
    private Boolean mustCopyPath;
    private GooglePhotosExporter.Granularity albumGranularity;


    public ExportSettings() {
    }

    public Boolean getMustAlwaysAskLocation() {
        return mustAlwaysAskLocation;
    }

    public void setMustAlwaysAskLocation(Boolean mustAlwaysAskLocation) {
        this.mustAlwaysAskLocation = mustAlwaysAskLocation;
    }

    public String getDestLocation() {
        return destLocation;
    }

    public void setDestLocation(String destLocation) {
        this.destLocation = destLocation;
    }

    public Boolean getMustRememberLastLocation() {
        return mustRememberLastLocation;
    }

    public void setMustRememberLastLocation(Boolean mustRememberLastLocation) {
        this.mustRememberLastLocation = mustRememberLastLocation;
    }

    public FileFormat getPreferredFileFormat() {
        return preferredFileFormat;
    }

    public void setPreferredFileFormat(FileFormat preferredFileFormat) {
        this.preferredFileFormat = preferredFileFormat;
    }

    public Boolean getMustShare() {
        return mustShare;
    }

    public void setMustShare(Boolean mustShare) {
        this.mustShare = mustShare;
    }

    public Boolean getMustCopyPath() {
        return mustCopyPath;
    }

    public void setMustCopyPath(Boolean mustCopyPath) {
        this.mustCopyPath = mustCopyPath;
    }

    public GooglePhotosExporter.Granularity getAlbumGranularity() {
        return albumGranularity;
    }

    public void setAlbumGranularity(GooglePhotosExporter.Granularity albumGranularity) {
        this.albumGranularity = albumGranularity;
    }


    /**
     * Fills the given Map with this object field's values
     * @param map the map to fill
     */
    public void copyToMap(Map<String,Object> map) {
        if (getMustAlwaysAskLocation() != null) map.put(MUST_ALWAYS_ASK_LOCATION_KEY, getMustAlwaysAskLocation());
        if (getDestLocation() != null) map.put(DEST_LOCATION_KEY, getDestLocation());
        if (getMustRememberLastLocation() != null) map.put(MUST_REMEMBER_LAST_LOCATION_KEY, getMustRememberLastLocation());
        if (getPreferredFileFormat() != null) map.put(PREFERRED_FILE_FORMAT_KEY, getPreferredFileFormat());
        if (getMustShare() != null) map.put(MUST_SHARE_KEY, getMustShare());
        if (getMustCopyPath() != null) map.put(MUST_COPY_PATH_KEY, getMustCopyPath());
        if (getAlbumGranularity() != null) map.put(ALBUM_GRANULARITY_KEY, getAlbumGranularity());
    }


    /**
     * Moves values from given map to distinct fields.
     * The given map is used to initialize fields of this object, and all such fields are removed from the map.
     * In other words, after this call, the map contains only fields that could not be imported.
     * @param map the source map. Will only contain un-imported settings on exit
     * @return the set of missing fields, that were not given in the provided map
     */
    public Set<String> moveFromMap(Map<String, Object> map) {

        Set<String> missingSettings = new HashSet<>();


        if (map.containsKey(MUST_ALWAYS_ASK_LOCATION_KEY)) {
            setMustAlwaysAskLocation((Boolean) map.get(MUST_ALWAYS_ASK_LOCATION_KEY));
            map.remove(MUST_ALWAYS_ASK_LOCATION_KEY);
        }
        else {
            missingSettings.add(MUST_ALWAYS_ASK_LOCATION_KEY);
        }


        if (map.containsKey(DEST_LOCATION_KEY)) {
            setDestLocation((String) map.get(DEST_LOCATION_KEY));
            map.remove(DEST_LOCATION_KEY);
        }
        else {
            missingSettings.add(DEST_LOCATION_KEY);
        }


        if (map.containsKey(MUST_REMEMBER_LAST_LOCATION_KEY)) {
            setMustRememberLastLocation((Boolean) map.get(MUST_REMEMBER_LAST_LOCATION_KEY));
            map.remove(MUST_REMEMBER_LAST_LOCATION_KEY);
        }
        else {
            missingSettings.add(MUST_REMEMBER_LAST_LOCATION_KEY);
        }


        if (map.containsKey(PREFERRED_FILE_FORMAT_KEY)) {
            setPreferredFileFormat((FileFormat) map.get(PREFERRED_FILE_FORMAT_KEY));
            map.remove(PREFERRED_FILE_FORMAT_KEY);
        }
        else {
            missingSettings.add(PREFERRED_FILE_FORMAT_KEY);
        }


        if (map.containsKey(MUST_SHARE_KEY)) {
            setMustShare((Boolean) map.get(MUST_SHARE_KEY));
            map.remove(MUST_SHARE_KEY);
        }
        else {
            missingSettings.add(MUST_SHARE_KEY);
        }


        if (map.containsKey(MUST_COPY_PATH_KEY)) {
            setMustCopyPath((Boolean) map.get(MUST_COPY_PATH_KEY));
            map.remove(MUST_COPY_PATH_KEY);
        }
        else {
            missingSettings.add(MUST_COPY_PATH_KEY);
        }


        if (map.containsKey(ALBUM_GRANULARITY_KEY)) {
            setAlbumGranularity((GooglePhotosExporter.Granularity) map.get(ALBUM_GRANULARITY_KEY));
            map.remove(ALBUM_GRANULARITY_KEY);
        }
        else {
            missingSettings.add(ALBUM_GRANULARITY_KEY);
        }


        return missingSettings;
    }
}
