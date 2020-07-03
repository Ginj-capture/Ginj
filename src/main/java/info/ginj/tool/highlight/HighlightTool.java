package info.ginj.tool.highlight;

import info.ginj.CaptureEditingFrame;
import info.ginj.ImageEditorPane;
import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;

import java.awt.*;

public class HighlightTool implements GinjTool {
    @Override
    public String getName() {
        return "Highlight";
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame, ImageEditorPane imagePane) {
        return new HighlightOverlay().initialize(initalPosition, initialColor);
    }
}
