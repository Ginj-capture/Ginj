package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.util.UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * This window allows a user to set the capture hotkey.
 */
public class HotKeyDefinitionDialog extends JDialog {

    // HotKey management
    public static final List<Integer> MODIFIERS = Arrays.asList(KeyEvent.VK_ALT, KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH);
    private final JTextField textField;
    private final JTextField hotKeyTextField;

    public HotKeyDefinitionDialog(StarWindow starWindow, JTextField hotKeyTextField) {
        super();
        this.hotKeyTextField = hotKeyTextField;
        setModal(true);

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " hotkey definition");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final JPanel contentPane = new DoubleBorderedPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        // Prepare title bar
        JPanel titleBar = UI.getTitleBar("Hotkey definition", e -> onOK());
        contentPane.add(titleBar, BorderLayout.NORTH);


        // Prepare main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

        mainPanel.add(new JLabel("Press hotkey combination"), BorderLayout.NORTH);
        JPanel inputPanel = new JPanel(new FlowLayout());

        textField = new JTextField(15);
        textField.setFont(textField.getFont().deriveFont(Font.BOLD, 15f));
        textField.setEditable(false);
        textField.setText(hotKeyTextField.getText());
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (MODIFIERS.contains(e.getKeyCode())) {
                    textField.setText("");
                }
                else {
                    textField.setText(KeyStroke.getKeyStrokeForEvent(e).toString().replaceAll("pressed ", ""));
                }
            }
        });
        inputPanel.add(textField);


        mainPanel.add(inputPanel, BorderLayout.CENTER);

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

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);
    }

    private void onOK() {
        hotKeyTextField.setText(textField.getText());
        // Close window
        dispose();
    }

    private void onCancel() {
        // Close window
        dispose();
    }

}
