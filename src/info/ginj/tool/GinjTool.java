package info.ginj.tool;

import java.awt.*;

public interface GinjTool {
    /**
     * Tool name, also used (in lowercase) to get the tool's icon
     * @return the Tool name
     */
    String getName();

    /**
     * Returns a component
     * @return
     * @param initalPosition
     * @param initialColor
     */
    Overlay createComponent(Point initalPosition, Color initialColor);
}
