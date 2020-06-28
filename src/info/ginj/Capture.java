package info.ginj;

import com.google.gson.annotations.Expose;
import info.ginj.tool.Overlay;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A capture is something (screenshot or screen recording) ready for export
 */
public class Capture {
    String id;
    boolean isVideo;
    String name;
    File file;
    BufferedImage originalImage;
    List<Overlay> overlays;
    BufferedImage renderedImage;
    List<Export> exports = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        this.isVideo = video;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
    }

    public List<Overlay> getOverlays() {
        return overlays;
    }

    public void setOverlays(List<Overlay> overlays) {
        this.overlays = overlays;
    }

    public BufferedImage getRenderedImage() {
        return renderedImage;
    }

    public void setRenderedImage(BufferedImage renderedImage) {
        this.renderedImage = renderedImage;
    }

    public List<Export> getExports() {
        return exports;
    }

    public void addExport(String exporter, String url, String id) {
        exports.add(new Export(exporter, id, url));
    }

    // Utils

    public String getType() {
        return isVideo ? "Video" : "Image";
    }

    /**
     * Returns the file of the capture, or write the BufferedImage to a temp file and return it if file was empty
     *
     * @return The file
     * @throws IOException in case file had to be created and an error occurred
     */
    public File toFile() throws IOException {
        if (file == null) {
            file = new File(Ginj.getTempDir(), id + ".png");
            ImageIO.write(renderedImage, "png", file);
            file.deleteOnExit();
        }
        return file;
    }

    /**
     * Returns the name of the capture, or its id if name is empty
     *
     * @return
     */
    public String getDefaultName() {
        if (name == null || name.isBlank()) {
            return id;
        }
        return name;
    }

    private class Export {
        @Expose
        String exporterName;
        @Expose
        String path;
        @Expose
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
    }
}
