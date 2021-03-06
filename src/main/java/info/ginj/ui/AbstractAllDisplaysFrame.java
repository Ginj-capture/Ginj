package info.ginj.ui;

import com.github.kokorin.jaffree.OS;
import info.ginj.model.DisplayConfiguration;
import info.ginj.model.Prefs;
import info.ginj.util.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import static info.ginj.model.Prefs.Key.USE_JNA_FOR_WINDOWS_MONITORS;

/**
 * This base class represents a frame that spans all available displays, a kind of "mega-full-screen window".
 * It is the base class for :
 * - the capture selection (which is a mega-full-screen window with the capture to crop as background)
 * - the video recording window (which is a mega-full-screen transparent window, but shares many aspects such as a rectangular overlay
 * and a self positioning button panel)
 */
public abstract class AbstractAllDisplaysFrame extends JFrame {
    // Caching
    protected DisplayConfiguration displayConfiguration;
    protected List<Rectangle> visibleAreas;
    protected Rectangle allDisplaysBounds;
    protected AffineTransform primaryDisplayTransform;
    protected boolean areTransformsUniform;

    protected StarWindow starWindow;
    protected JPanel actionPanel;

    // Current state
    protected Rectangle selection; // during capture selection, only filled when selection is done

    public AbstractAllDisplaysFrame(StarWindow starWindow, String windowTitle) {
        this.starWindow = starWindow;

        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        // For Alt+Tab behaviour
        this.setTitle(windowTitle);
        this.setIconImage(StarWindow.getAppIcon());

    }

    /**
     * Initialize and show the frame
     * As new JFrames reserve memory that is never garbage collected, we will reuse instances, and use open() and close()
     * to show or hide them, including initialization and cleanup.
     */
    protected void open() {
        // Hide the star widget
        starWindow.setVisible(false);

        // We suppose the display config will not change while the mega window is open... otherwise call it at regular interval...
        computeAllDisplayBounds();

        JComponent contentPane = createContentPane();
        setContentPane(contentPane);

        setLayout(null); // Allow absolute positioning of action panel

        // Prepare action panel with buttons
        actionPanel = createActionPanel();
        UI.packPanel(actionPanel);
        contentPane.add(actionPanel);

        pack();
        positionWindowOnStartup();
        setVisible(true);
        requestFocus();

        setAlwaysOnTop(true);
    }

    protected void close() {
        // Will allow garbage collection of the content pane and its subcomponents
        setContentPane(new JPanel());

        setVisible(false);

        dispose();

        // Restore the star widget
        starWindow.setVisible(true);
    }

    private void computeAllDisplayBounds() {
        displayConfiguration = UI.getDisplayConfiguration();

        // List "visible areas" of all monitors
        final boolean usePhysical = OS.IS_WINDOWS && Prefs.isTrue(USE_JNA_FOR_WINDOWS_MONITORS);
        visibleAreas = new ArrayList<>();
        AffineTransform previousTransform = displayConfiguration.getDisplayList().get(0).getDefaultTransform();
        areTransformsUniform = true;
        for (DisplayConfiguration.Display display : displayConfiguration.getDisplayList()) {
            if (usePhysical) {
                // Use physical value using JNA for Windows, to work around bug https://bugs.openjdk.java.net/browse/JDK-8211999 with scaled monitors
                visibleAreas.add(display.getPhysicalRectangle());
                if (!display.getDefaultTransform().equals(previousTransform)) {
                    areTransformsUniform = false;
                }
            }
            else {
                visibleAreas.add(display.getLogicalRectangle());
            }
        }

        // And Union them in a "mega-rectangle"
        allDisplaysBounds = null;
        for (Rectangle area : visibleAreas) {
            if (allDisplaysBounds == null) {
                // First rectangle, make a copy
                allDisplaysBounds = new Rectangle(area);
            }
            else {
                allDisplaysBounds.add(area);
            }
        }

        primaryDisplayTransform = displayConfiguration.getDisplayList().get(0).getDefaultTransform();
//        System.out.println("primaryDisplayTransform = " + primaryDisplayTransform);
    }


    private void positionWindowOnStartup() {
        setLocation(allDisplaysBounds.x, allDisplaysBounds.y);
    }

    protected abstract JComponent createContentPane();

    protected abstract JPanel createActionPanel();

    protected void positionActionPanel() {
        final int barWidth = actionPanel.getWidth();
        final int barHeight = actionPanel.getHeight();

        final int halfHorizontalStrokeWidth = getSelectedAreaHorizontalStrokeWidth() / 2;
        final int halfVerticalStrokeWidth = getSelectedAreaVerticalStrokeWidth() / 2;

        // Find the best position according to selection and bar size
        // Note: not the exact same strategy as the original, but close...
        // Note: all points are in the coordinates system of the "mega-window" so all are positive (top left is 0,0)
        // Note: these positions can probably be improved... later
        Point[] candidatePositions = new Point[]{
                new Point(selection.x, selection.y + selection.height + halfHorizontalStrokeWidth), // Below bottom left
                new Point(selection.x, selection.y - barHeight - halfHorizontalStrokeWidth), // Above top left
                new Point(selection.x - barWidth - halfVerticalStrokeWidth, selection.y), // Next to top left
                new Point(selection.x + selection.width + halfVerticalStrokeWidth, selection.y + selection.height - barHeight), // Next to bottom right
                new Point( /* TODO probably wrong when on secondary display */0, selection.y + selection.height), // Below, on left screen edge
                new Point( /* TODO probably wrong when on secondary display */0, selection.y - barHeight), // Above, on left screen edge
                new Point(selection.x + halfVerticalStrokeWidth, selection.y + selection.height - barHeight - 1), // Over, at bottom left of selection
                new Point(selection.x + halfVerticalStrokeWidth, selection.y + halfHorizontalStrokeWidth), // Over, at top left of selection
                new Point( /* TODO probably wrong when on secondary display */0, allDisplaysBounds.height - barHeight), // Over, at bottom left of captured area
                new Point( /* TODO probably wrong when on secondary display */0, allDisplaysBounds.height - barHeight), // Over, at top left of 1st screen
                new Point( /* TODO probably wrong when on secondary display */(int) (-allDisplaysBounds.x + (visibleAreas.get(0).getWidth() - barWidth) / 2), (int) (-allDisplaysBounds.y + (visibleAreas.get(0).getHeight() - barHeight) / 2)) // Last resort: at center of main screen
        };
        Point bestPosition = null;
        for (Point candidatePosition : candidatePositions) {
//            logger.info("Testing " + candidatePosition + "... ");
            // We have to apply the mega-window position offset to test this point in "window coordinates" against visibility in "device coordinates"
            final Rectangle candidateBounds = new Rectangle(allDisplaysBounds.x + candidatePosition.x, allDisplaysBounds.y + candidatePosition.y, barWidth, barHeight);
            if (allDisplaysBounds.contains(candidateBounds)) {
                // We have to take care of "checkered flag" multi screen configurations.
                // e.g. if we have one screen at top left and another one at bottom right,
                // then positioning the action panel at the bottom left is unacceptable as it will be unreachable with the mouse
                for (Rectangle visibleArea : visibleAreas) {
                    if (visibleArea.contains(candidateBounds)) {
                        bestPosition = candidatePosition;
//                        logger.info("On screen " + visibleArea + ": SELECTED !");
                        break;
                    }
//                    else {
//                        logger.info("Out of screen " + visibleArea + "... ");
//                    }
                }
                if (bestPosition != null) {
                    break;
                }
//                logger.info("Rejected.");
            }
//            else {
//                logger.info("Out of mega-rectangle: Rejected.");
//            }
        }
        if (bestPosition == null) {
            bestPosition = candidatePositions[candidatePositions.length - 1];
        }
        actionPanel.setBounds(new Rectangle(bestPosition.x, bestPosition.y, barWidth, barHeight));
    }

    protected abstract int getSelectedAreaHorizontalStrokeWidth();

    protected abstract int getSelectedAreaVerticalStrokeWidth();
}
