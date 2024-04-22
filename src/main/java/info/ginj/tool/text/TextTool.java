package info.ginj.tool.text;

import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.ui.CaptureEditingFrame;
import info.ginj.util.UI;

import java.awt.*;

public class TextTool extends GinjTool {

    public static final String NAME = "Text";

    // Remember color for new text overlays
    Color textColor = UI.TEXT_TOOL_DEFAULT_FOREGROUND_COLOR;

    public Color getTextColor() {
        return textColor;
    }

    // TODO should be called upon overlay text color change
    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame) {
        final TextOverlay overlay = new TextOverlay().initialize(initalPosition, initialColor);
        overlay.setTextColor(getTextColor());
        overlay.setFrame(frame);
        return overlay;
    }
}
