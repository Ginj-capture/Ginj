package info.ginj.tool.text;

import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;

import java.awt.*;

public class GinjTextTool implements GinjTool {
    @Override
    public String getName() {
        return "Text";
    }

    @Override
    public Overlay createComponent(Point initalPosition, Color initialColor) {
        return new GinjText().initialize(initalPosition, initialColor);
    }
}