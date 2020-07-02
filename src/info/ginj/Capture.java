package info.ginj;

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
    List<Overlay> overlays;
    List<Export> exports = new ArrayList<>();
    File file;
    BufferedImage originalImage;
    BufferedImage renderedImage;


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

    public List<Overlay> getOverlays() {
        return overlays;
    }

    public void setOverlays(List<Overlay> overlays) {
        this.overlays = overlays;
    }

    public List<Export> getExports() {
        return exports;
    }

    public void setExports(List<Export> exports) {
        this.exports = exports;
    }

    public void addExport(String exporter, String url, String id) {
        exports.add(new Export(exporter, id, url));
    }

    /**
     * Getter renamed to non-javabeans convention so it is skipped by XMLEncoder
     */
    public File transientGetFile() {
        return file;
    }

    /**
     * Setter renamed to non-javabeans convention so it is skipped by XMLDecoder
     */
    public void transientSetFile(File file) {
        this.file = file;
    }

    /**
     * Getter renamed to non-javabeans convention so it is skipped by XMLEncoder
     */
    public BufferedImage transientGetOriginalImage() {
        return originalImage;
    }

    /**
     * Setter renamed to non-javabeans convention so it is skipped by XMLDecoder
     */
    public void transientSetOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
    }

    /**
     * Getter renamed to non-javabeans convention so it is skipped by XMLEncoder
     */
    public BufferedImage transientGetRenderedImage() {
        return renderedImage;
    }

    /**
     * Setter renamed to non-javabeans convention so it is skipped by XMLDecoder
     */
    public void transientSetRenderedImage(BufferedImage renderedImage) {
        this.renderedImage = renderedImage;
    }

    // Utils

    public String getType() {
        return isVideo ? "Video" : "Image";
    }

    /**
     * Returns the file of the capture, or writes the BufferedImage to a temp file and returns it if file was empty
     *
     * @return The file
     * @throws IOException in case file had to be created and an error occurred
     */
    public File toFile() throws IOException {
        if (file == null) {
            file = new File(Ginj.getTempDir(), id + Ginj.IMAGE_EXTENSION);
            ImageIO.write(renderedImage, Ginj.IMAGE_FORMAT_PNG, file);
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

    @Override
    public String toString() {
        return "Capture{" +
                "id='" + id + '\'' +
                ", isVideo=" + isVideo +
                ", name='" + name + '\'' +
                '}';
    }
}
