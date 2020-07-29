package info.ginj.tool;

import info.ginj.tool.arrow.ArrowTool;
import info.ginj.tool.frame.FrameTool;
import info.ginj.tool.highlight.HighlightTool;
import info.ginj.tool.oval.OvalTool;
import info.ginj.tool.text.TextTool;
import info.ginj.ui.CaptureEditingFrame;
import info.ginj.ui.ImageEditorPane;

import java.awt.*;
import java.beans.Transient;
import java.util.Map;
import java.util.TreeMap;

/**
 * A tool is a class that creates overlays
 */
public abstract class GinjTool {

    private static Map<String, GinjTool> toolMap;

    /**
     * This static method returns a map of all tools.
     *
     * @return a map containing an instance of each "tool", indexed by tool name
     */
    @Transient
    public static Map<String, GinjTool> getMap() {
        if (toolMap == null) {
            toolMap = new TreeMap<>();
            toolMap.put(ArrowTool.NAME, new ArrowTool());
            toolMap.put(TextTool.NAME, new TextTool());
            toolMap.put(FrameTool.NAME, new FrameTool());
            toolMap.put(OvalTool.NAME, new OvalTool());
            toolMap.put(HighlightTool.NAME, new HighlightTool());
        }
        return toolMap;
    }

    /**
     * Tool name, also used (in lowercase) to get the tool's icon
     *
     * @return the Tool name
     */
    public abstract String getName();

    /**
     * Returns a new Overlay
     *
     * @param initalPosition
     * @param initialColor
     * @param frame
     * @param imagePane
     * @return
     */
    public abstract Overlay createComponent(Point initalPosition, Color initialColor, CaptureEditingFrame frame, ImageEditorPane imagePane);
}
