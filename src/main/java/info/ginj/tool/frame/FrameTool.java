package info.ginj.tool.frame;

import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.ui.CaptureEditingFrame;
import info.ginj.ui.ImageEditorPane;

import java.awt.*;

public class FrameTool implements GinjTool {
    @Override
    public String getName() {
        return "Frame";
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame, ImageEditorPane imagePane) {
        return new FrameOverlay().initialize(initalPosition, initialColor);
    }
}
