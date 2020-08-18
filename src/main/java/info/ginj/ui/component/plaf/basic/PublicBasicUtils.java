package info.ginj.ui.component.plaf.basic;

import javax.swing.plaf.ComponentUI;

public class PublicBasicUtils {

    /**
     * Returns the ui that is of type <code>klass</code>, or null if
     * one can not be found.
     *
     * Copied from javax.swing.plaf.basic.BasicLookAndFeel because this method is not public there (??)
     */
    public static Object getUIOfType(ComponentUI ui, Class<?> klass) {
        if (klass.isInstance(ui)) {
            return ui;
        }
        return null;
    }

}
