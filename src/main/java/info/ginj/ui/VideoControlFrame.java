package info.ginj.ui;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.tulskiy.keymaster.common.Provider;
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
    public static final int BORDER_WIDTH = 4;

    private final StarWindow starWindow;

    private final JLabel captureDurationLabel;

    private FFmpegResultFuture ffmpegFutureResult = null;

    public VideoControlFrame(StarWindow starWindow, Rectangle croppedSelection) {
        super();
        this.starWindow = starWindow;
        // Hide the widget
        starWindow.setVisible(false);

        // Start recording right away
        startRecording(croppedSelection);

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " recording");
        this.setIconImage(StarWindow.getAppIcon());

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        // The window itself is transparent
        setBackground(new Color(0, 0, 0, 0));
        // And must be always on top
        setAlwaysOnTop(true);

        // Prepare the control panel
        final JPanel controlPanel = new DoubleBorderedPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));
        final JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> onStop());
        controlPanel.add(stopButton);
        captureDurationLabel = new JLabel("00:00:00");
        controlPanel.add(captureDurationLabel);
        Dimension controlPanelSize = UI.packPanel(controlPanel);

        // Prepare the main panel to contain the capture area, a border around it, and the control panel
        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(UI.AREA_SELECTION_COLOR);
                g2d.setStroke(new BasicStroke(BORDER_WIDTH, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10,10}, 0.0f));
                g2d.drawRect(BORDER_WIDTH / 2,BORDER_WIDTH / 2, croppedSelection.width + BORDER_WIDTH, croppedSelection.height + BORDER_WIDTH);
            }
        };
        setContentPane(contentPane);
        contentPane.setOpaque(false);
        contentPane.setLayout(null);
        // Position it so that it encloses the area to record (relative to the display)
        Rectangle windowBounds = new Rectangle(croppedSelection.x - BORDER_WIDTH, croppedSelection.y - BORDER_WIDTH, croppedSelection.width + 2 * BORDER_WIDTH, croppedSelection.height + 2 * BORDER_WIDTH + controlPanelSize.height);
        setBounds(windowBounds);


        // Position the control panel under the area to record (relative to the contentPane)
        contentPane.add(controlPanel);
        controlPanel.setBounds(BORDER_WIDTH, BORDER_WIDTH * 2 + croppedSelection.height, controlPanelSize.width, controlPanelSize.height);
    }

    private void startRecording(Rectangle croppedSelection) {
        String ffmpegDir = Prefs.get(Prefs.Key.FFMPEG_BIN_DIR);
        FFmpeg ffmpeg;
        if (ffmpegDir != null) {
            ffmpeg = FFmpeg.atPath(Paths.get(ffmpegDir));
        }
        else {
            // Suppose it's on the path
            ffmpeg = FFmpeg.atPath();
        }

        int frameRate;
        try {
            frameRate = Integer.parseInt(Prefs.get(Prefs.Key.VIDEO_FRAMERATE));
        }
        catch (Exception e) {
            frameRate = 10;
        }

        boolean captureMouseCursor = Prefs.isTrue(Prefs.Key.VIDEO_CAPTURE_MOUSE_CURSOR);

        int finalFrameRate = frameRate;
        ProgressListener progressListener = progress -> {
            // Notes:
            // progress.getTime() is about encoding and has an offset of 5-10 sec compared to actual capture time
            // progress.getFrame() is accurate
            // progress.getFps() can be irrelevant at startup before it settles
            // So using requested frameRate to convert frames to seconds.
            // TODO what if fps setting is too high and encoding can't keep up. Can we detect it with a drop in progress.getFps() compared to frameRate ?
            Duration elapsed = Duration.ofMillis(1000 * progress.getFrame() / finalFrameRate);
            captureDurationLabel.setText(String.format("%02d:%02d:%02d", elapsed.toHours(), elapsed.toMinutesPart(), elapsed.toSecondsPart()));
        };

        if (SystemUtils.IS_OS_MAC) {
            // avfoundation on mac supports a crop width/height but no offset (silly isn't it), so ffmpeg has to capture the full desktop and crop in a separate step:
            ffmpegFutureResult = ffmpeg
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
            ffmpegFutureResult = ffmpeg
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

        // The capture window will lose focus during recording as user interacts with his apps.
        // Make sure we detect CTRL-S for Stop and ESC for Cancel
        addGlobalRecordingHotkeys();
    }

    private String getTempVideoFilename() {
        return Ginj.getTempDir().getAbsolutePath() + File.separator + "capture.mp4";
    }

    private void stopRecording() {
        ffmpegFutureResult.graceStop();
        removeGlobalRecordingHotkeys();
        starWindow.registerHotKey();
    }

    private void addGlobalRecordingHotkeys() {
        Provider provider = starWindow.getHotkeyProvider();
        provider.reset();
        // TODO add these hotkeys to the Prefs and Options dialog
        provider.register(KeyStroke.getKeyStroke("ctrl S"), hotKey -> onStop());
        provider.register(KeyStroke.getKeyStroke("ESCAPE"), hotKey -> onCancel());
    }

    private void removeGlobalRecordingHotkeys() {
        starWindow.getHotkeyProvider().reset();
    }

    @Override
    public void dispose() {
        // Restore Widget
        starWindow.setVisible(true);
        super.dispose();
    }


    ///////////////////////
    // Event handlers

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

    private void onStop() {
        stopRecording();
        // TODO open capture editing
        dispose();
    }


}
