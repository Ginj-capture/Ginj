package info.ginj.action;

import javax.swing.undo.AbstractUndoableEdit;

public abstract class AbstractUndoableAction extends AbstractUndoableEdit {
    public abstract void execute();
}
