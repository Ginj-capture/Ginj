package info.ginj.tool.arrow;

import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.ui.CaptureEditingFrame;

import java.awt.*;

public class ArrowTool extends GinjTool {

    public static final String NAME = "Arrow";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame) {
        return new ArrowOverlay().initialize(initalPosition, initialColor);
    }
}
