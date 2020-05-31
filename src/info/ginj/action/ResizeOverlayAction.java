package info.ginj.action;

import info.ginj.tool.Overlay;

import java.awt.*;

public class ResizeOverlayAction extends AbstractUndoableAction {
    private final Overlay overlay;
    private final int handleIndex;
    private final Point initialPosition;
    private Point finalPosition;

    public ResizeOverlayAction(Overlay overlay, int handleIndex, Point initialPosition) {
        this.overlay = overlay;
        this.handleIndex = handleIndex;
        this.initialPosition = initialPosition;
    }

    public String getPresentationName() {
        return "resize " + overlay.getPresentationName().toLowerCase();
    }

    public void setTargetPoint(Point finalPosition) {
        this.finalPosition = finalPosition;
    }


    public void execute() {
        overlay.moveHandle(handleIndex, finalPosition);
    }


    public void undo() {
        super.undo();
        overlay.moveHandle(handleIndex, initialPosition);
    }

    public void redo() {
        super.redo();
        execute();
    }

}
