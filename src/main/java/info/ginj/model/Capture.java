package info.ginj.model;

import info.ginj.Ginj;
import info.ginj.tool.Overlay;
import info.ginj.util.Misc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.beans.Transient;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A capture is something (screenshot or screen recording) ready for export
 */
public class Capture implements Cloneable {
    public static final String VERSION_SEPARATOR = "_v";

    String id;
    int version = 1;
    boolean isVideo = false;
    String name;
    List<Overlay> overlays = new ArrayList<>();
    List<Export> exports = new ArrayList<>();
    File originalFile;
    BufferedImage originalImage;
    File renderedFile;
    BufferedImage renderedImage;
    long videoDurationMs;
    long videoLowerBoundMs;
    long videoHigherBoundMs;

    public Capture() {
    }

    public Capture(String id) {
        this.id = id;
    }

    public Capture(String id, BufferedImage image) {
        this.id = id;
        this.originalImage = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public void addExport(Export export) {
        exports.add(export);
    }


    public void setVideoDurationMs(long videoDurationMs) {
        this.videoDurationMs = videoDurationMs;
    }

    public long getVideoDurationMs() {
        return videoDurationMs;
    }

    public long getVideoLowerBoundMs() {
        return videoLowerBoundMs;
    }

    public void setVideoLowerBoundMs(long videoLowerBoundMs) {
        this.videoLowerBoundMs = videoLowerBoundMs;
    }

    public long getVideoHigherBoundMs() {
        return videoHigherBoundMs;
    }

    public void setVideoHigherBoundMs(long videoHigherBoundMs) {
        this.videoHigherBoundMs = videoHigherBoundMs;
    }

    // Note: Transient to prevent being saved to disk

    @Transient
    public File getOriginalFile() {
        return originalFile;
    }

    @Transient
    public void setOriginalFile(File originalFile) {
        this.originalFile = originalFile;
    }

    @Transient
    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    @Transient
    public void setOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
    }

    @Transient
    public File getRenderedFile() {
        return renderedFile;
    }

    @Transient
    public void setRenderedFile(File renderedFile) {
        this.renderedFile = renderedFile;
    }

    @Transient
    public BufferedImage getRenderedImage() {
        return renderedImage;
    }

    @Transient
    public void setRenderedImage(BufferedImage renderedImage) {
        this.renderedImage = renderedImage;
    }

    @Override
    public Capture clone() throws CloneNotSupportedException {
        return (Capture)super.clone();
    }


// Utils

    public String computeExtension() {
        return isVideo()? Misc.VIDEO_EXTENSION : Misc.IMAGE_EXTENSION;
    }


    /**
     * Returns the file of the capture, or writes the BufferedImage to a temp file and returns it if file was empty
     *
     * @return The file
     * @throws IOException in case file had to be created and an error occurred
     */
    public File toRenderedFile() throws IOException {
        if (renderedFile == null) {
            renderedFile = new File(Ginj.getTempDir(), id + Misc.IMAGE_EXTENSION);
            ImageIO.write(renderedImage, Misc.IMAGE_FORMAT_PNG, renderedFile);
            renderedFile.deleteOnExit();
        }
        return renderedFile;
    }

    @Transient
    public String getType() {
        return isVideo ? "Video" : "Image";
    }

    /**
     * Returns the name of the capture, or its id if name is empty
     *
     * @return
     */
    @Transient
    public String getDefaultName() {
        if (name == null || name.isBlank()) {
            return getBaseFilename();
        }
        return name;
    }

    @Transient
    public String getBaseFilename() {
        String baseFilename = getId();
        if (getVersion() > 1) {
            baseFilename += VERSION_SEPARATOR + getVersion();
        }
        return baseFilename;
    }

    @Override
    public String toString() {
        return "Capture{" +
                "id='" + id + '\'' +
                ", version=" + version +
                ", isVideo=" + isVideo +
                ", name='" + name + '\'' +
                '}';
    }

    /**
     * Compute the name under which the capture will be shared
     * @return the name to use
     */
    public String computeUploadFilename() {
        String filename = getDefaultName();
        String extension = computeExtension();
        if (!filename.toLowerCase().endsWith(extension)) {
            filename += extension;
        }
        return filename;
    }
}
