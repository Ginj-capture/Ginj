package info.ginj;

import java.io.Serializable;

public class Export implements Serializable {
    String exporterName;
    String path;
    String mediaId;

    public Export() {
    }

    public Export(String exporterName, String mediaId, String path) {
        this.exporterName = exporterName;
        this.path = path;
        this.mediaId = mediaId;
    }

    public String getExporterName() {
        return exporterName;
    }

    public void setExporterName(String exporterName) {
        this.exporterName = exporterName;
    }

    /**
     * Path can be null (clipboard), a file path (for saved files), or a URL (for shared files)
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
                ", path='" + path + '\'' +
                ", mediaId='" + mediaId + '\'' +
                '}';
    }
}
