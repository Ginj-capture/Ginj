package info.ginj.tool.frame;

import info.ginj.ImageEditorPane;
import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;

import java.awt.*;

public class FrameTool implements GinjTool {
    @Override
    public String getName() {
        return "Frame";
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor, ImageEditorPane imagePane) {
        return new FrameOverlay().initialize(initalPosition, initialColor);
    }
}
