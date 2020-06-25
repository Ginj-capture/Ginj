package info.ginj;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A capture is something (screenshot or screen recording ready for export
 */
public class Capture {

    String id;
    String name;
    File file;
    BufferedImage image;
    boolean isVideo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        this.isVideo = video;
    }

    // Utils

    public String getType() {
        return isVideo ?"Video":"Image";
    }

    /**
     * Returns the file of the capture, or write the BufferedImage to a temp file and return it if file was empty
     * @return The file
     * @throws IOException in case file had to be created and an error occurred
     */
    public File toFile() throws IOException {
        if (file == null) {
            file = new File(Ginj.getTempDir(), id + ".png");
            ImageIO.write(image, "png", file);
            file.deleteOnExit();
        }
        return file;
    }

    /**
     * Returns the name of the capture, or its id if name is empty
     * @return
     */
    public String getDefaultName() {
        if (name == null || name.isBlank()) {
            return id;
        }
        return name;
    }

}
