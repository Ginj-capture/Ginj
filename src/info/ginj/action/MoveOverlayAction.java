package info.ginj.action;

import info.ginj.tool.Overlay;

import java.awt.*;

public class MoveOverlayAction extends AbstractUndoableAction {
    private final Overlay overlay;
    private final Point initialPosition;
    private Point finalPosition;

    public MoveOverlayAction(Overlay overlay, Point initialPosition) {
        this.overlay = overlay;
        this.initialPosition = initialPosition;
    }

    public String getPresentationName() {
        return "move " + overlay.getPresentationName().toLowerCase();
    }

    public void setTargetPoint(Point finalPosition) {
        this.finalPosition = finalPosition;
    }

    public void execute() {
        overlay.moveDrawing(finalPosition.x - initialPosition.x, finalPosition.y - initialPosition.y);
    }

    public void undo() {
        super.undo();
        overlay.moveDrawing(- finalPosition.x + initialPosition.x, - finalPosition.y + initialPosition.y);
    }

    public void redo() {
        super.redo();
        execute();
    }

}
