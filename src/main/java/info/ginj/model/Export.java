package info.ginj.model;

import java.io.Serializable;

public class Export implements Serializable {
    String exporterName;
    String location;
    String mediaId;

    public Export() {
    }

    public Export(String exporterName, String mediaId, String location) {
        this.exporterName = exporterName;
        this.location = location;
        this.mediaId = mediaId;
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

    @Override
    public String toString() {
        return "Export{" +
                "exporterName='" + exporterName + '\'' +
                ", path='" + location + '\'' +
                ", mediaId='" + mediaId + '\'' +
                '}';
    }
}
