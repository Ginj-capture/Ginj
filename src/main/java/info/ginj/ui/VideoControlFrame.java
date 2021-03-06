package info.ginj.ui;

import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.tulskiy.keymaster.common.Provider;
import info.ginj.Ginj;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;
import info.ginj.ui.component.BorderedLabel;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.ui.component.LowerButton;
import info.ginj.ui.component.LowerButtonBar;
import info.ginj.util.Jaffree;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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

    public static final int SELECTED_AREA_STROKE_WIDTH = 2;

    public static final int FILM_PERFORATION_BAND_WIDTH_PX = 16;
    public static final int FILM_PERFORATION_WIDTH_PX = 8;
    public static final int FILM_PERFORATION_HEIGHT_PX = 10;
    public static final int FILM_PERFORATION_SPACE_PX = 6;
    public static final int FILM_PERFORATION_VERTICAL_INTERVAL = FILM_PERFORATION_HEIGHT_PX + FILM_PERFORATION_SPACE_PX;
    public static final int SELECTION_TOP_BOTTOM_BORDER_WIDTH = 2;
    public static final BasicStroke SELECTION_TOP_BOTTOM_STROKE = new BasicStroke(2);


    private JLabel captureDurationLabel;

    private FFmpegResultFuture ffmpegFutureResult = null;
    private Capture capture;

    private Timer cellPerforationAnimationTimer;
    private BufferedImage perforationImage;
    private int perforationOffset = 0;

    public VideoControlFrame(StarWindow starWindow) {
        super(starWindow, Ginj.getAppName() + " recording");

        // Prepare film perforations animation

        perforationImage = new BufferedImage(FILM_PERFORATION_BAND_WIDTH_PX, FILM_PERFORATION_VERTICAL_INTERVAL, BufferedImage.TYPE_INT_RGB);
        final Graphics2D perforationGraphics = (Graphics2D) perforationImage.getGraphics();
        // Background
        perforationGraphics.setColor(UI.AREA_SELECTION_COLOR);
        perforationGraphics.fillRect(0, 0, perforationImage.getWidth(), perforationImage.getHeight());
        // Perforations
        perforationGraphics.setColor(Color.BLACK);
        perforationGraphics.setRenderingHints(UI.ANTI_ALIASING_ON);
        perforationGraphics.fillRoundRect((FILM_PERFORATION_BAND_WIDTH_PX - FILM_PERFORATION_WIDTH_PX) / 2, 0, FILM_PERFORATION_WIDTH_PX, FILM_PERFORATION_HEIGHT_PX, 6, 6);
        perforationGraphics.dispose();
    }

    public void init(Rectangle selection, Capture capture) {
        this.selection = selection;
        this.capture = capture;
    }

    @Override
    public void open() {
        super.open();

        // Start recording
        capture.setOriginalFile(new File(getTempVideoFilename()));
        Rectangle videoSelection = new Rectangle(this.selection); // Copy: only the video selection must be translated
        videoSelection.translate(allDisplaysBounds.x, allDisplaysBounds.y);
        startRecording(videoSelection);

        // The window itself is transparent
        setBackground(new Color(0, 0, 0, 0));

        positionActionPanel();
        actionPanel.setVisible(true);
        captureDurationLabel.setText("00:00:00");

        cellPerforationAnimationTimer = new Timer(50, e -> {
            perforationOffset++;
            if (perforationOffset >= FILM_PERFORATION_HEIGHT_PX + FILM_PERFORATION_SPACE_PX) {
                perforationOffset = 0;
            }
            repaint();
        });
        cellPerforationAnimationTimer.start();
    }

    @Override
    public void close() {
        cellPerforationAnimationTimer.stop();
        cellPerforationAnimationTimer = null;
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        ffmpegFutureResult = null;
        capture = null;
        perforationImage = null;

        super.close();
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

            // top/bottom borders
            g2d.setStroke(SELECTION_TOP_BOTTOM_STROKE);
            g2d.drawLine(selection.x, selection.y - SELECTION_TOP_BOTTOM_BORDER_WIDTH/2, selection.x + selection.width, selection.y - SELECTION_TOP_BOTTOM_BORDER_WIDTH/2);
            g2d.drawLine(selection.x, selection.y + selection.height + SELECTION_TOP_BOTTOM_BORDER_WIDTH/2, selection.x + selection.width, selection.y + selection.height + SELECTION_TOP_BOTTOM_BORDER_WIDTH/2);

            // left/right perforations
            g2d.setPaint(new TexturePaint(perforationImage, new Rectangle(selection.x - FILM_PERFORATION_BAND_WIDTH_PX, -perforationOffset, perforationImage.getWidth(), perforationImage.getHeight())));
            g2d.fillRect(selection.x - FILM_PERFORATION_BAND_WIDTH_PX, selection.y - SELECTION_TOP_BOTTOM_BORDER_WIDTH, FILM_PERFORATION_BAND_WIDTH_PX, selection.height + 2 * SELECTION_TOP_BOTTOM_BORDER_WIDTH);
            g2d.setPaint(new TexturePaint(perforationImage, new Rectangle(selection.x + selection.width, -perforationOffset, perforationImage.getWidth(), perforationImage.getHeight())));
            g2d.fillRect(selection.x + selection.width, selection.y - SELECTION_TOP_BOTTOM_BORDER_WIDTH, FILM_PERFORATION_BAND_WIDTH_PX, selection.height + 2 * SELECTION_TOP_BOTTOM_BORDER_WIDTH);
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
    protected int getSelectedAreaHorizontalStrokeWidth() {
        return SELECTION_TOP_BOTTOM_BORDER_WIDTH;
    }

    @Override
    protected int getSelectedAreaVerticalStrokeWidth() {
        return FILM_PERFORATION_BAND_WIDTH_PX;
    }

    private void startRecording(Rectangle croppedSelection) {

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

        // The capture window will lose focus during recording as user interacts with the desktop and apps.
        // Make sure we detect CTRL-S for Stop and ESC for Cancel
        setGlobalRecordingHotkeys();

        // Start actual recording
        ffmpegFutureResult = Jaffree.startRecording(croppedSelection, frameRate, captureMouseCursor, progressListener, capture.getOriginalFile().getAbsolutePath());
    }

    private String getTempVideoFilename() {
        return Ginj.getTempDir().getAbsolutePath() + File.separator + capture.getId() + ".mp4";
    }

    private boolean stopRecording() {
        // Restore hotkey handling
        removeGlobalRecordingHotkeys();
        starWindow.registerHotKey();

        // Wait and make sure the process has ended
        return Jaffree.stopRecording(ffmpegFutureResult, logger);
    }

    private void setGlobalRecordingHotkeys() {
        Provider provider = starWindow.getHotkeyProvider();
        provider.reset();
        // TODO add these hotkeys to the Prefs and Options dialog
        provider.register(KeyStroke.getKeyStroke("ctrl S"), hotKey -> onStop());
        provider.register(KeyStroke.getKeyStroke("ESCAPE"), hotKey -> onCancel());
    }

    private void removeGlobalRecordingHotkeys() {
        starWindow.getHotkeyProvider().reset();
    }


    ///////////////////////
    // Event handlers

    private void onCancel() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        stopRecording();
        File videoFile = capture.getOriginalFile();
        if (videoFile.exists()) {
            if (!videoFile.delete()) {
                logger.trace("Could not delete video file '" + videoFile.getAbsolutePath() + "'.");
            }
        }
        close();
    }

    private void onStop() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (stopRecording()) {
            final long videoDurationMs = Jaffree.getDuration(capture.getOriginalFile());
            capture.setVideoDurationMs(videoDurationMs);
            capture.setVideoHigherBoundMs(videoDurationMs);
            // Open capture editing
            final CaptureEditingFrame captureEditingFrame = CaptureEditingFrame.getInstance(starWindow);
            captureEditingFrame.open(capture);
        }
        close();
    }

}
