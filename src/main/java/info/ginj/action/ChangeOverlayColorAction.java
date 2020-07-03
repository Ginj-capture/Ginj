package info.ginj.action;

import info.ginj.tool.Overlay;

import java.awt.*;

public class ChangeOverlayColorAction extends AbstractUndoableAction {
    private final Overlay overlay;
    private final Color newColor;
    private final Color originalColor;


    public ChangeOverlayColorAction(Overlay overlay, Color newColor) {
        super();
        this.overlay = overlay;
        originalColor = overlay.getColor();
        this.newColor = newColor;
    }

    public String getPresentationName() {
        return "change " + overlay.getPresentationName().toLowerCase() + " color";
    }

    public void execute() {
        overlay.setColor(newColor);
    }

    public void undo() {
        super.undo();
        overlay.setColor(originalColor);
    }

    public void redo() {
        super.redo();
        execute();
    }

}
