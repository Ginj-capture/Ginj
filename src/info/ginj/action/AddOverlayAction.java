package info.ginj.action;

import info.ginj.tool.Overlay;

import javax.swing.*;

public class AddOverlayAction extends AbstractUndoableAction {

    private final Overlay overlay;
    private final JLayeredPane panel;

    public AddOverlayAction(Overlay overlay, JLayeredPane panel) {
        super();
        this.overlay = overlay;
        this.panel = panel;
    }

    public String getPresentationName() {
        return "Create " + overlay.getName().toLowerCase();
    }

    public void execute() {
        overlay.setSelected(true);
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
