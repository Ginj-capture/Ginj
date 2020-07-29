package info.ginj.tool.oval;

import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.ui.CaptureEditingFrame;
import info.ginj.ui.ImageEditorPane;

import java.awt.*;

public class OvalTool extends GinjTool {

    public static final String NAME = "Oval";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame, ImageEditorPane imagePane) {
        return new OvalOverlay().initialize(initalPosition, initialColor);
    }
}
