package info.ginj.tool.frame;

import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.ui.CaptureEditingFrame;

import java.awt.*;

public class FrameTool extends GinjTool {

    public static final String NAME = "Frame";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame) {
        return new FrameOverlay().initialize(initalPosition, initialColor);
    }
}
