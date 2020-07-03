package info.ginj.action;

import javax.swing.undo.AbstractUndoableEdit;
import java.awt.*;

public abstract class AbstractUndoableAction extends AbstractUndoableEdit {
    public abstract void execute();

    public void setTargetPoint(Point point) {
        // default empty implementation
    }

    public String toString() {
        return super.toString() + " (" + getPresentationName() + ")";
    }
}
