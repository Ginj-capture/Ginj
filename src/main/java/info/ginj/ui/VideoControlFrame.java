package info.ginj.ui;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import info.ginj.Ginj;
import info.ginj.model.Prefs;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.util.UI;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * This frame immediately starts the video capture and shows stop/restart/etc for Video Recording
 */
public class VideoControlFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(VideoControlFrame.class);

    private final StarWindow starWindow;
    private final JLabel captureDurationLabel;

    public VideoControlFrame(StarWindow starWindow, Rectangle croppedSelection) {
        super();
        this.starWindow = starWindow;

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " recording");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final JPanel contentPane = new DoubleBorderedPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new FlowLayout());
        contentPane.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));
        final JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> onStop());
        contentPane.add(stopButton);

        captureDurationLabel = new JLabel("000000");
        contentPane.add(captureDurationLabel);

        // Lay out components again
        pack();

        UI.addEscKeyShortcut(this, e -> onCancel());

        // Center window TODO change
        starWindow.centerFrameOnStarIconDisplay(this);

        startRecording(croppedSelection);
    }

    FFmpegResultFuture futureResult = null;

    private void startRecording(Rectangle croppedSelection) {
        FFmpeg ffmpeg;
        String ffmpegDir = Prefs.get(Prefs.Key.FFMPEG_BIN_DIR);
        if (ffmpegDir != null) {
            ffmpeg = FFmpeg.atPath(Paths.get(ffmpegDir));
        }
        else {
            ffmpeg = FFmpeg.atPath();
        }

        int frameRate;
        try {
            frameRate = Integer.parseInt(Prefs.get(Prefs.Key.VIDEO_FRAMERATE));
        }
        catch (Exception e) {
            frameRate = 10;
        }

        boolean captureMouseCursor = Prefs.isTrue(Prefs.Key.VIDEO_CAPTURE_CURSOR);

        ProgressListener progressListener = new ProgressListener() {
            @Override
            public void onProgress(FFmpegProgress progress) {
                Duration elapsed = Duration.ofMillis(progress.getTimeMillis());
                captureDurationLabel.setText(String.format("%02d:%02d:%02d", elapsed.toHours(), elapsed.toMinutesPart(), elapsed.toSecondsPart()));
            }
        };

        if (SystemUtils.IS_OS_MAC) {
            // avfoundation on mac supports a crop width/height but no offset (silly isn't it), so ffmpeg has to capture the full desktop and crop in a separate step:
            futureResult = ffmpeg
                    .addInput(CaptureInput
                            .captureDesktop()
                            .setCaptureFrameRate(frameRate)
                            .setCaptureCursor(captureMouseCursor)
                    )
                    .setFilter(StreamType.VIDEO, "crop=" + croppedSelection.width + ":" + croppedSelection.height + ":" + croppedSelection.x + ":" + croppedSelection.y)
                    .setProgressListener(progressListener)
                    .addOutput(UrlOutput.toPath(Paths.get(getTempVideoFilename())))
                    .setOverwriteOutput(true)
                    .executeAsync();
        }
        else {
            // OTOH on Windows' GDIGrab and Linux' X11Grab, cropping is supported at the input level :
            futureResult = ffmpeg
                    .addInput(CaptureInput
                            .captureDesktop()
                            .setCaptureFrameRate(frameRate)
                            .setCaptureCursor(captureMouseCursor)
                            .setCaptureVideoOffset(croppedSelection.x, croppedSelection.y)
                            .setCaptureVideoSize(croppedSelection.width, croppedSelection.height)
                    )
                    .setProgressListener(progressListener)
                    .addOutput(UrlOutput.toPath(Paths.get(getTempVideoFilename())))
                    .setOverwriteOutput(true)
                    .executeAsync();
        }
    }

    private String getTempVideoFilename() {
        return Ginj.getTempDir().getAbsolutePath() + File.separator + "capture.mp4";
    }

    private void stopRecording() {
        futureResult.graceStop();
    }

    private void onStop() {
        stopRecording();
        // TODO send to capture editing
        dispose();
    }

    private void onCancel() {
        stopRecording();
        File videoFile = new File(getTempVideoFilename());
        if (videoFile.exists()) {
            boolean deleted = false;
            int numTries = 0;
            while (!deleted && numTries < 5) {
                deleted = videoFile.delete();
                if (!deleted) {
                    logger.trace("Could not delete video file '" + videoFile.getAbsolutePath() + "' at try " + numTries);
                    numTries++;
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        // noop;
                    }
                }
            }
        }
        dispose();
    }

}
