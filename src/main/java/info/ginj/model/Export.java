package info.ginj.model;

import java.io.Serializable;

public class Export implements Serializable {
    private String exporterName;
    private String location;
    private String mediaId;
    private boolean isLocationCopied;

    public Export() {
    }

    public Export(String exporterName, String mediaId, String location, boolean isLocationCopied) {
        this.exporterName = exporterName;
        this.location = location;
        this.mediaId = mediaId;
        this.isLocationCopied = isLocationCopied;
    }

    public String getExporterName() {
        return exporterName;
    }

    public void setExporterName(String exporterName) {
        this.exporterName = exporterName;
    }

    /**
     * Location can be null (clipboard), a file path (for saved files), or a URL (for shared files)
     *
     * @return
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public boolean isLocationCopied() {
        return isLocationCopied;
    }

    public void setLocationCopied(boolean locationCopied) {
        isLocationCopied = locationCopied;
    }

    @Override
    public String toString() {
        return "Export{" +
                "exporterName='" + exporterName + '\'' +
                ", location='" + location + '\'' +
                ", mediaId='" + mediaId + '\'' +
                ", isLocationCopied=" + isLocationCopied +
                '}';
    }
}
