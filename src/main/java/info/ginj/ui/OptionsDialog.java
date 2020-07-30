package info.ginj.ui;

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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * This window allows a user to set her or his preferences
 */
public class OptionsDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(OptionsDialog.class);

    public static final List<Integer> HOTKEY_MODIFIERS = Arrays.asList(KeyEvent.VK_ALT, KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH);
    public static final String HOTKEY_CLICK_HERE_PLACEHOLDER = "Click here";
    public static final String HOTKEY_PRESS_KEYS_PLACEHOLDER = "Press desired key(s)";

    private static StarWindow starWindow;
    private final JTextField hotKeyTextField;
    private final JCheckBox useTrayNotificationsOnExportCompletion;
    private final JCheckBox ovalOverlayCheckBox;

    private KeyStroke hotKey;

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
        hotKeyTextField.setEditable(false);
        String hotKeyStr = Prefs.get(Prefs.Key.CAPTURE_HOTKEY);
        if (hotKeyStr != null) {
            hotKey = KeyStroke.getKeyStroke(hotKeyStr);
            hotKeyTextField.setText(hotKeyStr);
        }
        else {
            hotKey = null;
            hotKeyTextField.setText(HOTKEY_CLICK_HERE_PLACEHOLDER);
        }
        hotKeyTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // This method is called once for each key (e.g. once for ctrl, once for shift and once for P)
                // e.getKeyCode() is only the last pressed key. If the last pressed key was a modifier, it's not a valid combination.
                if (HOTKEY_MODIFIERS.contains(e.getKeyCode())) {
                    hotKey = null;
                    hotKeyTextField.setText(HOTKEY_PRESS_KEYS_PLACEHOLDER);
                }
                else {
                    // A "non-modifier" key was pressed last. Remember the combination.
                    hotKey = KeyStroke.getKeyStrokeForEvent(e);
                    hotKeyTextField.setText(toKeyString(hotKey));
                }
            }
        });

        // Change the message according to the focus (click here / press keys)
        hotKeyTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (hotKey == null) {
                    hotKeyTextField.setText(HOTKEY_PRESS_KEYS_PLACEHOLDER);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (hotKey == null) {
                    hotKeyTextField.setText(HOTKEY_CLICK_HERE_PLACEHOLDER);
                }
            }
        });
        hotKeyFieldPanel.add(hotKeyTextField);

        JButton hotKeyDefineButton = new JButton("Clear");
        hotKeyDefineButton.addActionListener((e -> onClearHotKey()));
        hotKeyFieldPanel.add(hotKeyDefineButton);

        useTrayNotificationsOnExportCompletion = new JCheckBox();
        useTrayNotificationsOnExportCompletion.setSelected(Prefs.isTrue(Prefs.Key.USE_TRAY_NOTIFICATION_ON_EXPORT_COMPLETION));

        ovalOverlayCheckBox = new JCheckBox();
        GinjTool ovalTool = GinjTool.getMap().get(OvalTool.NAME);
        ovalOverlayCheckBox.setSelected(Prefs.getToolSet().contains(ovalTool));

        // Add fields to main panel
        mainPanel.add(UI.createFieldPanel(
                "Capture hotkey", hotKeyFieldPanel,
                "Enable Oval Overlay", ovalOverlayCheckBox,
                "Use tray notification on export", useTrayNotificationsOnExportCompletion
        ));

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

    private String toKeyString(KeyStroke keyStroke) {
        // We're in the "keyPressed" handler, so remove the word "pressed " from the toString() version of the combination
        return keyStroke.toString().replaceAll("pressed ", "");
    }

    private void onClearHotKey() {
        hotKey = null;
        hotKeyTextField.setText(HOTKEY_CLICK_HERE_PLACEHOLDER);
    }

    private void onOK() {

        if (hotKey == null) {
            logger.info("Removing capture hotkey");
            Prefs.remove(Prefs.Key.CAPTURE_HOTKEY);
            //Prefs.set(Prefs.Key.CAPTURE_HOTKEY, "ctrl PRINTSCREEN");
        }
        else {
            String hotKeyString = toKeyString(hotKey);
            logger.info("Setting capture hotkey to " + hotKeyString);
            Prefs.set(Prefs.Key.CAPTURE_HOTKEY, hotKeyString);
        }


        Prefs.set(Prefs.Key.USE_TRAY_NOTIFICATION_ON_EXPORT_COMPLETION, String.valueOf(useTrayNotificationsOnExportCompletion.isSelected()));


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
