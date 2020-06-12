package info.ginj.ui;

import info.ginj.CaptureEditingFrame;
import info.ginj.Prefs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ColorSelectorWindow extends JWindow {

    private CaptureEditingFrame frame;

    public ColorSelectorWindow(CaptureEditingFrame captureEditingFrame) {
        super(captureEditingFrame);
        this.frame = captureEditingFrame;

        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(3, 3));

        JPanel buttonPanel = new JPanel(new GridLayout(3, 3, 3, 3));
        for (int buttonNumber = 0; buttonNumber < 9; buttonNumber++) {
            final GinjColorToggleButton colorToggleButton = new GinjColorToggleButton(Prefs.getColorWithSuffix(Prefs.Key.FIXED_PALETTE_COLOR_PREFIX, String.valueOf(buttonNumber)));
            colorToggleButton.addActionListener(e -> {
                frame.setCurrentColor(colorToggleButton.getColor());
                dispose();
            });
            buttonPanel.add(colorToggleButton);
        }
        contentPane.add(buttonPanel, BorderLayout.CENTER);

        JButton customColorButton = new JButton("Custom...");
        customColorButton.addActionListener(e -> {
            // TODO Open colorChooser
            // TODO Upon selection, store color in list of recently used colors (unless it's already the last one)
        });
        contentPane.add(customColorButton, BorderLayout.SOUTH);

        // Close when losing focus
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                ColorSelectorWindow.this.dispose();
            }
        });

        pack();
    }
}
