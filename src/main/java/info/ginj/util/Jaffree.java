package info.ginj.util;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.*;
import info.ginj.model.Prefs;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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

    public static void stopRecording(FFmpegResultFuture ffmpegFutureResult, Logger logger) {
        // Gently request ffmpeg to end (by pressing "q")
        ffmpegFutureResult.graceStop();

        long timeout = Prefs.getAsLong(Prefs.Key.FFMPEG_TERMINATION_TIMEOUT, 10);
        try {
            ffmpegFutureResult.get(timeout, TimeUnit.SECONDS);
            return;
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
//        try {
//            Thread.sleep(1000);
//        }
//        catch (InterruptedException e) {
//            e.printStackTrace();
//        }
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
