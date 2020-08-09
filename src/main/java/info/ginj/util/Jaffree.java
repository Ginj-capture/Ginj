package info.ginj.util;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.*;
import info.ginj.Ginj;
import info.ginj.model.Prefs;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This util class gathers all interaction with Jaffree for FFmpeg interaction
 */
public class Jaffree {

    private static final Logger logger = LoggerFactory.getLogger(Jaffree.class);

    /**
     * Indicates if video captures can be performed (filled by testing ffmpeg availability)
     */
    public static boolean IS_AVAILABLE = false;


    public static void checkAvailability() {
        String ffmpegExecutable = "ffmpeg";
        if (SystemUtils.IS_OS_WINDOWS) {
            ffmpegExecutable = "ffmpeg.exe";
        }

        // 1. folder specified in preference file ?
        String ffmpegBinDirname = Prefs.get(Prefs.Key.FFMPEG_BIN_DIR);
        if (ffmpegBinDirname != null) {
            if ((new File(new File(ffmpegBinDirname), ffmpegExecutable)).exists()) {
                logger.info("Ffmpeg found, specified in prefs file");
                IS_AVAILABLE = true;
                return;
            }
        }

        // 2. distributed with Ginj ?
        try {
            File ffmpegBinDir = new File(new File(Jaffree.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent(), "ffmpeg");
            if ((new File(ffmpegBinDir, ffmpegExecutable)).exists()) {
                logger.info("Ffmpeg found, installed with " + Ginj.getAppName());
                IS_AVAILABLE = true;
                // Remember it
                Prefs.set(Prefs.Key.FFMPEG_BIN_DIR, ffmpegBinDir.getAbsolutePath());
                return;
            }
        }
        catch (URISyntaxException e) {
            // Should never happen
            logger.error("Error trying to run the version of ffmpeg installed with " + Ginj.getAppName(), e);
        }

        // 3. on the path? Just try and see
        try {
            // This does throw an exception because there's no output:
//            FFmpeg.atPath().addArgument("-version").execute();
            // Temporary alternative, run ffmpeg without Jaffree
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            logger.info("Ffmpeg found, on the path (" + output.readLine() + ")");
            IS_AVAILABLE = true;
        }
        catch (Exception e) {
            logger.warn("Error trying to execute ffmpeg from the path");
        }
    }

    public static FFmpeg getFFmpeg() {
        String ffmpegDir = Prefs.get(Prefs.Key.FFMPEG_BIN_DIR);
        FFmpeg ffmpeg;
        if (ffmpegDir != null) {
            ffmpeg = FFmpeg.atPath(Paths.get(ffmpegDir));
        }
        else {
            // Assume ffmpeg is on the path TODO check it
            ffmpeg = FFmpeg.atPath();
        }
        return ffmpeg;
    }


    public static FFmpegResultFuture startRecording(Rectangle area, int frameRate, boolean captureMouseCursor, ProgressListener progressListener, String videoFilename) {
        if (SystemUtils.IS_OS_MAC) {
            // avfoundation on mac supports a crop width/height but no offset (silly isn't it ?), so ffmpeg has to capture the full desktop and crop in a separate step:
            return getFFmpeg()
                    .addInput(CaptureInput
                            .captureDesktop()
                            .setCaptureFrameRate(frameRate)
                            .setCaptureCursor(captureMouseCursor)
                    )
                    .setFilter(StreamType.VIDEO, "crop=" + area.width + ":" + area.height + ":" + area.x + ":" + area.y)
                    .setProgressListener(progressListener)
                    .addOutput(UrlOutput.toPath(Paths.get(videoFilename)))
                    .setOverwriteOutput(true)
                    .executeAsync();
        }
        else {
            // OTOH on Windows' GDIGrab and Linux' X11Grab, cropping is supported at the input level :
            return getFFmpeg()
                    .addInput(CaptureInput
                            .captureDesktop()
                            .setCaptureFrameRate(frameRate)
                            .setCaptureCursor(captureMouseCursor)
                            .setCaptureVideoOffset(area.x, area.y)
                            .setCaptureVideoSize(area.width, area.height)
                    )
                    .setProgressListener(progressListener)
                    .addOutput(UrlOutput.toPath(Paths.get(videoFilename)))
                    .setOverwriteOutput(true)
                    .executeAsync();
        }
    }

    public static boolean stopRecording(FFmpegResultFuture ffmpegFutureResult, Logger logger) {
        // Gently request ffmpeg to end (by pressing "q")
        ffmpegFutureResult.graceStop();

        long timeout = Prefs.getAsLong(Prefs.Key.FFMPEG_TERMINATION_TIMEOUT, 15);
        try {
            ffmpegFutureResult.get(timeout, TimeUnit.SECONDS);
            return true;
        }
        catch (InterruptedException | ExecutionException e) {
            UI.alertException(null, "Recording error", "There was an error waiting for recording to complete", e, logger);
        }
        catch (TimeoutException e) {
            UI.alertException(null, "Recording error", "The recording did not complete after " + timeout + " seconds.", e, logger);
        }
        try {
            ffmpegFutureResult.forceStop();
        }
        catch (Exception exception) {
            // Jaffree is known to cause a RuntimeException when forcing desktop capture stop.
            // See https://github.com/kokorin/Jaffree/issues/91
            logger.error("Exception occurred during forceStop()", exception);
        }
        return false;
    }

    public static BufferedImage grabImage(File file, long positionInMillis) {
        final AtomicLong trackCounter = new AtomicLong();
        final AtomicLong frameCounter = new AtomicLong();

        // Use an array to pass a final variable although its contents will be changed
        final BufferedImage[] images = new BufferedImage[1];

        FrameConsumer consumer = new FrameConsumer() {
            @Override
            public void consumeStreams(List<Stream> tracks) {
                trackCounter.set(tracks.size());
            }

            @Override
            public void consume(Frame frame) {
                if (frame == null) {
                    return;
                }
                images[0] = frame.getImage();
            }
        };

        getFFmpeg()
                .addInput(
                        UrlInput.fromPath(file.toPath())
                                .setPosition(positionInMillis, TimeUnit.MILLISECONDS)
                                .setDuration(1, TimeUnit.SECONDS)
                )
                .addOutput(
                        FrameOutput.withConsumer(consumer)
                                .setFrameCount(StreamType.VIDEO, 1L)
                                .setFrameRate(1) // 1 frame per second
                                .disableStream(StreamType.AUDIO)
                                .disableStream(StreamType.SUBTITLE)
                                .disableStream(StreamType.DATA)
                )
                .execute();

        return images[0];
    }

    public static long getDuration(File file) {
        final AtomicLong durationMillis = new AtomicLong();
        getFFmpeg()
                .addInput(UrlInput.fromPath(file.toPath()))
                .addOutput(new NullOutput())
                .setProgressListener(progress -> {
                    System.out.println("hello");
                    durationMillis.set(progress.getTimeMillis());
                })
                .execute();
        return durationMillis.get();
    }
}
