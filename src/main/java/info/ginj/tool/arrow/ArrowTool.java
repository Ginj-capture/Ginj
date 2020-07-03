package info.ginj.tool.arrow;

import info.ginj.CaptureEditingFrame;
import info.ginj.ImageEditorPane;
import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;

import java.awt.*;

public class ArrowTool implements GinjTool {
    @Override
    public String getName() {
        return "Arrow";
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame, ImageEditorPane imagePane) {
        return new ArrowOverlay().initialize(initalPosition, initialColor);
    }
}
