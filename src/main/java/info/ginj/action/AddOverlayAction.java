package info.ginj.action;

import info.ginj.tool.Overlay;
import info.ginj.ui.ImageEditorPane;

public class AddOverlayAction extends AbstractUndoableAction {

    private final Overlay overlay;
    private final ImageEditorPane panel;

    public AddOverlayAction(Overlay overlay, ImageEditorPane panel) {
        super();
        this.overlay = overlay;
        this.panel = panel;
    }

    public String getPresentationName() {
        return "create " + overlay.getPresentationName().toLowerCase();
    }

    public void execute() {
        panel.setSelectedOverlay(overlay);
        panel.add(overlay, Integer.valueOf(panel.highestLayer() + 1));
    }

    public void undo() {
        super.undo();
        panel.remove(overlay);
    }

    public void redo() {
        super.redo();
        execute();
    }
}
