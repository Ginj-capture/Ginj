package info.ginj.action;

import info.ginj.ImageEditorPane;
import info.ginj.tool.Overlay;

import javax.swing.*;

public class BringOverlayToFrontAction extends AbstractUndoableAction {
    private final Overlay overlay;
    private final ImageEditorPane panel;
    private final int originalLayer;


    public BringOverlayToFrontAction(Overlay overlay, ImageEditorPane panel) {
        super();
        this.overlay = overlay;
        this.panel = panel;
        originalLayer = JLayeredPane.getLayer(overlay);
    }

    public String getPresentationName() {
        return "bring " + overlay.getPresentationName().toLowerCase() + " to front";
    }

    public void execute() {
        panel.setLayer(overlay, panel.highestLayer() + 1);
    }

    public void undo() {
        super.undo();
        panel.setLayer(overlay, originalLayer);
    }

    public void redo() {
        super.redo();
        execute();
    }

}
