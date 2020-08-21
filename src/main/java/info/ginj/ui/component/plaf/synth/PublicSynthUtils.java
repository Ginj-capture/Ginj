package info.ginj.ui.component.plaf.synth;

import javax.swing.plaf.synth.SynthLookAndFeel;

public class PublicSynthUtils extends SynthLookAndFeel {
    /**
     * A convience method that will reset the Style of StyleContext if
     * necessary.
     *
     * Copied from javax.swing.plaf.synth.SynthLookAndFeel because this method is not public there (??)
     *
     * @return newStyle
     */
/*
    public static SynthStyle updateStyle(SynthContext context, SynthUI ui) {

        SynthStyle newStyle = getStyle(context.getComponent(),
                context.getRegion());
        SynthStyle oldStyle = context.getStyle();

        if (newStyle != oldStyle) {
            if (oldStyle != null) {
                oldStyle.uninstallDefaults(context);
            }
            context.setStyle(newStyle);
            newStyle.installDefaults(context, ui);
        }
        return newStyle;
    }
*/
}
