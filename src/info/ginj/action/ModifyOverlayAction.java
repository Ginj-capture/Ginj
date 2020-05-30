package info.ginj.action;

import info.ginj.tool.Overlay;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;

public class ModifyOverlayAction extends AbstractUndoableEdit {
    private final Overlay overlay;
    private final JLayeredPane panel;

    public ModifyOverlayAction(Overlay overlay, JLayeredPane panel) {
        this.overlay = overlay;
        this.panel = panel;
    }

    public String getPresentationName() {
        return "Modify " + overlay.getName().toLowerCase();
    }

    public void execute() {
//            overlay.setSelected(true);
//            panel.add(overlay, Integer.valueOf(panel.getComponentCount()));
    }


    public void undo() {
        super.undo();
        // undo the change
    }

    public void redo() {
        super.redo();
        execute();
    }

}
