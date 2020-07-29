package info.ginj.ui;

import com.tulskiy.keymaster.common.Provider;
import info.ginj.Ginj;
import info.ginj.model.Prefs;
import info.ginj.tool.GinjTool;
import info.ginj.tool.oval.OvalTool;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Set;


/**
 * This window allows a user to set her or his preferences
 */
public class OptionsDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(OptionsDialog.class);

    private static StarWindow starWindow = null;
    private final JTextField hotKeyTextField;
    private final JCheckBox ovalOverlayCheckBox;

    public OptionsDialog(StarWindow starWindow) {
        super();
        OptionsDialog.starWindow = starWindow;

        // When entering a modal dialog, hotkey must be disabled, otherwise the app gets locked
        starWindow.unregisterHotKey();
        setModal(true);

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " options");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final JPanel contentPane = new DoubleBorderedPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        // Prepare title bar
        JPanel titleBar = UI.getTitleBar("Options", e -> onCancel());
        contentPane.add(titleBar, BorderLayout.NORTH);


        // Prepare main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

        // Prepare hotkey field
        JPanel hotKeyFieldPanel = new JPanel(new FlowLayout());
        hotKeyTextField = new JTextField(15);
        hotKeyTextField.setEnabled(false);
        String hotKey = Prefs.get(Prefs.Key.CAPTURE_HOTKEY);
        if (hotKey != null) {
            hotKeyTextField.setText(hotKey);
        }
        hotKeyFieldPanel.add(hotKeyTextField);
        JButton hotKeyDefineButton = new JButton("...");
        hotKeyDefineButton.addActionListener((e -> onDefineHotKey()));
        hotKeyFieldPanel.add(hotKeyDefineButton);

        ovalOverlayCheckBox = new JCheckBox();
        GinjTool ovalTool = GinjTool.getMap().get(OvalTool.NAME);
        ovalOverlayCheckBox.setSelected(Prefs.getToolSet().contains(ovalTool));

        // Add fields to main panel
        mainPanel.add(UI.createFieldPanel(
                "Capture hotkey", hotKeyFieldPanel,
                "Enable Oval Overlay", ovalOverlayCheckBox));

        // TODO add Capture folder

        // Add main panel to dialog
        contentPane.add(mainPanel, BorderLayout.CENTER);


        // Prepare lower panel
        JPanel lowerPanel = new JPanel();
        lowerPanel.setOpaque(false);
        lowerPanel.setLayout(new FlowLayout());

        final JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> onOK());
        lowerPanel.add(okButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());
        lowerPanel.add(cancelButton);

        contentPane.add(lowerPanel, BorderLayout.SOUTH);

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        //setPreferredSize(WINDOW_PREFERRED_SIZE);
        pack();

        UI.addEscKeyShortcut(this, e -> onCancel());

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    private void onDefineHotKey() {
        final Provider provider = starWindow.getHotkeyProvider();
        if (provider == null) {
            // This is the first time a user will encounter hotKey code, so warn him in case of provider issue.
            // Apart from here, a null provider is silently ignored
            UI.alertError(this, "HotKey error", "Error initializing hotkey provider.");
        }
        else {
            (new HotKeyDefinitionDialog(starWindow, hotKeyTextField)).setVisible(true);
        }
    }

    private void onOK() {
        String newHotKey = hotKeyTextField.getText();
        if (newHotKey != null && !newHotKey.equals(Prefs.get(Prefs.Key.CAPTURE_HOTKEY))) {
            // Hotkey changed
            if (newHotKey.length() == 0) {
                logger.info("Removing capture hotkey");
                Prefs.remove(Prefs.Key.CAPTURE_HOTKEY);
            }
            else {
                Prefs.set(Prefs.Key.CAPTURE_HOTKEY, newHotKey);
            }
        }

        GinjTool ovalTool = GinjTool.getMap().get(OvalTool.NAME);
        Set<GinjTool> toolSet = Prefs.getToolSet();
        if (ovalOverlayCheckBox.isSelected()) {
            toolSet.add(ovalTool);
        }
        else {
            toolSet.remove(ovalTool);
        }
        Prefs.setToolSet(toolSet);

        Prefs.save();

        // Exiting modal dialog: Restore the hotkey behaviour (previous or new)
        starWindow.registerHotKey();

        // Close window
        dispose();
    }


    private void onCancel() {
        // Restore the previous hotkey
        starWindow.registerHotKey();
        // Close window
        dispose();
    }
}
