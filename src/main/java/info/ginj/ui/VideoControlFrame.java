package info.ginj.ui;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.tulskiy.keymaster.common.Provider;
import info.ginj.Ginj;
import info.ginj.model.Prefs;
import info.ginj.ui.component.BorderedLabel;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.ui.component.LowerButton;
import info.ginj.ui.component.LowerButtonBar;
import info.ginj.util.UI;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * This frame immediately starts the video recording and shows a frame around the recorded area,
 * plus stop/cancel/etc controls.
 * Note that for simplicity, this frame covers the full display and is fully transparent, except for the frame and controls.
 * Using this trick, we can freely move the control area where there is space, using the same strategy as when adjusting the
 * capture selection
 */
public class VideoControlFrame extends AbstractAllDisplaysFrame {

    private static final Logger logger = LoggerFactory.getLogger(VideoControlFrame.class);

    public static final int SELECTED_AREA_STROKE_WIDTH = 4;
    public static final BasicStroke SELECTED_AREA_STROKE = new BasicStroke(SELECTED_AREA_STROKE_WIDTH, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10, 10}, 0.0f);


    private JLabel captureDurationLabel;

    private FFmpegResultFuture ffmpegFutureResult = null;

    public VideoControlFrame(StarWindow starWindow, Rectangle selection) {
        super(starWindow, Ginj.getAppName() + " recording");
        this.selection = selection;

        // Hide the widget
        starWindow.setVisible(false);

        // Start recording right away
        startRecording(this.selection);

        // The window itself is transparent
        setBackground(new Color(0, 0, 0, 0));

        positionActionPanel();
        actionPanel.setVisible(true);
        captureDurationLabel.setText("00:00:00");
    }

    @Override
    protected JComponent createContentPane() {
        return new RecordingMainPane();
    }

    public class RecordingMainPane extends JPanel {
        public RecordingMainPane() {
            super();
            setOpaque(false);
            setLayout(null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(UI.AREA_SELECTION_COLOR);
            g2d.setStroke(SELECTED_AREA_STROKE);
            g2d.drawRect(selection.x - SELECTED_AREA_STROKE_WIDTH / 2, selection.y - SELECTED_AREA_STROKE_WIDTH / 2, selection.width + SELECTED_AREA_STROKE_WIDTH, selection.height + SELECTED_AREA_STROKE_WIDTH);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(allDisplaysBounds.width, allDisplaysBounds.height);
        }

    }

    @Override
    protected JPanel createActionPanel() {
        // Prepare the control panel
        JPanel actionPanel = new DoubleBorderedPanel(); // To add a margin around buttonBar
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 2));
        JPanel buttonBar = new LowerButtonBar();

        final JButton stopButton = new LowerButton("Finish", UI.createIcon(getClass().getResource("/img/icon/stop.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        stopButton.addActionListener(e -> onStop());
        buttonBar.add(stopButton);
//        final JButton pauseButton = new LowerButton("Pause", UI.createIcon(getClass().getResource("/img/icon/pause.png"), 16, 16, UI.ICON_ENABLED_COLOR));
//        pauseButton.addActionListener(e -> onPause());
//        buttonBar.add(pauseButton);
//        final JButton unmuteButton = new LowerButton("Unmute", UI.createIcon(getClass().getResource("/img/icon/unmute.png"), 16, 16, UI.ICON_ENABLED_COLOR));
//        pauseButton.addActionListener(e -> onUnmute());
//        buttonBar.add(pauseButton);
//        final JButton redoButton = new LowerButton("Restart", UI.createIcon(getClass().getResource("/img/icon/redo_selection.png"), 16, 16, UI.ICON_ENABLED_COLOR));
//        redoButton.addActionListener(e -> onRestart());
//        buttonBar.add(redoButton);
        final JButton cancelButton = new LowerButton("Cancel", UI.createIcon(getClass().getResource("/img/icon/cancel.png"), 16, 16, UI.ICON_ENABLED_COLOR));
        cancelButton.addActionListener(e -> onCancel());
        buttonBar.add(cancelButton);
        captureDurationLabel = new BorderedLabel("000:00:00");
        Font font = captureDurationLabel.getFont();
        captureDurationLabel.setFont(new Font(font.getName(), font.getStyle(), 18));
        buttonBar.add(captureDurationLabel);

        actionPanel.add(buttonBar);
        return actionPanel;
    }

    @Override
    protected int getSelectedAreaStrokeWidth() {
        return SELECTED_AREA_STROKE_WIDTH;
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
