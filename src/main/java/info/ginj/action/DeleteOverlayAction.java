package info.ginj.action;

import info.ginj.tool.Overlay;
import info.ginj.ui.ImageEditorPane;

import javax.swing.*;

public class DeleteOverlayAction extends AbstractUndoableAction {

    private final Overlay overlay;
    private final ImageEditorPane panel;
    private final int layer;

    public DeleteOverlayAction(Overlay overlay, ImageEditorPane panel) {
        super();
        this.overlay = overlay;
        this.panel = panel;
        this.layer = JLayeredPane.getLayer(overlay);
    }

    public String getPresentationName() {
        return "delete " + overlay.getPresentationName().toLowerCase();
    }

    public void execute() {
        panel.remove(overlay);
    }

    public void undo() {
        super.undo();
        panel.add(overlay, layer);
        panel.setSelectedOverlay(overlay);
    }

    public void redo() {
        super.redo();
        execute();
    }

}
