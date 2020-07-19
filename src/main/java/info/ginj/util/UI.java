package info.ginj.util;

import info.ginj.Ginj;
import info.ginj.ui.layout.SpringLayoutUtilities;
import info.ginj.ui.listener.DragInsensitiveMouseClickListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class UI {
    public static final Color AREA_SELECTION_COLOR = new Color(251, 185, 1);
    public static final Color SELECTION_SIZE_BOX_COLOR = new Color(0, 0, 0, 128);
    public static final Color UNSELECTED_AREA_DIMMED_COLOR = new Color(144, 144, 144, 112);

    public static final Color ICON_ENABLED_COLOR = new Color(243,205,77);
    public static final Color TOOLBAR_ICON_ENABLED_COLOR = new Color(238,179,8);
    public static final Color TEXT_TOOL_DEFAULT_FOREGROUND_COLOR = Color.BLACK;

    public static final Color HISTORY_SELECTED_ITEM_BACKGROUND_COLOR = new Color(255, 208, 42);

    public static final Color HANDLE_CENTER_COLOR = new Color(72, 72, 212, 128);
    public static final Color HANDLE_GREY_1_COLOR = new Color(253, 253, 253, 128);
    public static final Color HANDLE_GREY_2_COLOR = new Color(242, 242, 242, 128);
    public static final Color HANDLE_GREY_3_COLOR = new Color(180, 180, 180, 128);
    public static final Color HANDLE_GREY_4_COLOR = new Color(136, 136, 136, 128);

    // Used for web pages
    public static final Color LABEL_BACKGROUND_COLOR = new Color(27, 29, 30);
    public static final Color LABEL_FOREGROUND_COLOR = new Color(222, 165, 5);

    public static final String[] SIZE_UNITS = {" KiB", " MiB", " GiB", " TiB", " PiB"};

    /**
     * Lay out components of a Panel and compute its size, like pack() for a Window.
     * This method computes the size of the given panel by adding it to a temporary window.
     * Warning, must be called before adding the panel to its final parent, because it will be removed from it otherwise
     * @return the size of the panel when packed
     */
    public static Dimension packPanel(JPanel panel) {
        JWindow window = new JWindow();
        window.setLayout(new BorderLayout());
        window.getContentPane().add(panel);
        window.pack();
        final Dimension size = window.getSize();
        panel.setSize(size);
        return size;
    }

    /**
     * Create an ImageIcon of the given width x height from an image loaded from the given URL
     * @param resource the URL of the source image
     * @param width the desired width
     * @param height the desired height
     * @return the scaled ImageIcon
     */
    public static ImageIcon createIcon(URL resource, int width, int height) {
        return createIcon(resource, width, height, null);
    }

    /**
     * Create an ImageIcon of the given width (and scaled proportionally) from an image loaded from the given URL
     * @param resource the URL of the source image
     * @param width the desired width
     * @return the scaled ImageIcon
     */
    public static ImageIcon createIcon(URL resource, int width) {
        return createIcon(resource, width, -1, null);
    }

    /**
     * Create an ImageIcon of the given width x height and with the given base color from an image loaded from the given URL
     * @param resource the URL of the source image
     * @param width the desired width
     * @param height the desired height (if -1, image is scaled proportionally to reach the desired width)
     * @param color the desired base color to shift this image to (if null, no tint change is applied)
     * @see #tint(BufferedImage, Color) for more info about color shift
     * @return the scaled ImageIcon
     */
    public static ImageIcon createIcon(URL resource, int width, int height, Color color) {
        try {
            BufferedImage image = ImageIO.read(resource);
            if (color != null) {
                image = tint(image, color);
            }
            if (height == -1) {
                height = (width * image.getHeight()) / image.getWidth();
            }
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }
        catch (IOException e) {
            System.err.println("Error loading resource: " + resource);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Apply the given color shift to the given image.
     * Basically, the given color will replace black in the source image, and all greyscale levels in the source will be be mapped between this color and white
     * @param source the source image
     * @param color the base color to shift to
     * @return the colored image
     */
    public static BufferedImage tint(BufferedImage source, Color color) {
        final BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final int minRed = color.getRed();
        final int minGreen = color.getGreen();
        final int minBlue = color.getBlue();

        final double redFactor = (255 - minRed) / 255.0;
        final double greenFactor = (255 - minGreen) / 255.0;
        final double blueFactor = (255 - minBlue) / 255.0;

        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                Color pixelColor = new Color(source.getRGB(x, y), true);
                int r = (int) (minRed + (pixelColor.getRed() * redFactor));
                int g = (int) (minGreen + (pixelColor.getGreen() * greenFactor));
                int b = (int) (minBlue + (pixelColor.getBlue() * blueFactor));
                int a = pixelColor.getAlpha();

                result.setRGB(x, y, new Color(r,g,b,a).getRGB());
            }
        }
        return result;
    }

    /**
     * Convert an image to greyscale and apply a dim effect to it.
     * Used to show unselected area when drawing selection
     * @param image the source image
     * @return the dimmed result
     */
    public static Image makeDimmedImage(BufferedImage image) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        final BufferedImage greyScale = op.filter(image, null);
        final Graphics graphics = greyScale.getGraphics();
        graphics.setColor(UNSELECTED_AREA_DIMMED_COLOR);
        graphics.fillRect(0,0,image.getWidth(), image.getHeight());
        graphics.dispose();
        return greyScale;
    }

    /**
     * Make a color translucent, that is the same RGB but with half opacity
     * @param color the source color
     * @return the translucent color
     */
    public static Color getTranslucentColor(Color color) {
        // Make 50% opacity
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
    }

    @SuppressWarnings("SameParameterValue")
    public static Icon createRectColorIcon(Color color, int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRect(x, y, width, height);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return width;
            }

            @Override
            public int getIconHeight() {
                return height;
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    public static Icon createRoundRectColorIcon(Color color, int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(x, y, width, height, width / 3, height / 3);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return width;
            }

            @Override
            public int getIconHeight() {
                return height;
            }
        };
    }

    /**
     * Converts a color to its hex representation.
     * <p>
     * @see java.awt.Color#decode(String) is the opposite operation
     * @param color the color to convert
     * @return the hex value in the form #RRGGBB
     */
    public static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static JEditorPane createClickableHtmlEditorPane(String htmlMessage) {
        // for copying style
        JLabel label = new JLabel();
        Font font = label.getFont();

        // html content
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {font-family:" + font.getFamily() + ";"
                + "font-weight:" + (font.isBold() ? "bold" : "normal") + ";"
                + "font-size:" + font.getSize() + "pt;}");
        styleSheet.addRule("a {color: #FBB901;}");

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditorKit(kit);
        editorPane.setText("<html><body>" + htmlMessage + "</body></html>");

        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                }
                catch (Exception e1) {
                    // noop
                }
            }
        });
        editorPane.setEditable(false);
        return editorPane;
    }

    public static void addDraggableWindowMouseBehaviour(JFrame frame, Component handle) {
        MouseInputListener mouseListener = new MouseInputAdapter() {
            Point clicked;

            public void mousePressed(MouseEvent e) {
                clicked = e.getPoint();
            }

            public void mouseDragged(MouseEvent e) {
                Point position = e.getPoint();
                Point location = frame.getLocation();
                int x = location.x - clicked.x + position.x;
                int y = location.y - clicked.y + position.y;
                frame.setLocation(x, y);
            }
        };
        handle.addMouseListener(mouseListener);
        handle.addMouseMotionListener(mouseListener);
    }

    /**
     * Creates a panel representing the title bar
     * @param title text to display at the middle of the window
     * @param closeListener if specified, a close button (X) is added on the right, and this listener is called upon click.
     *                      if null, no close button is displayed
     * @return the title bar component
     */
    // TODO fix styling and unify the two cases (w and w/o close)
    public static JPanel getTitleBar(String title, ActionListener closeListener) {
        JPanel titleBar = new JPanel();
        titleBar.setName("TitleBarPanel"); // to be addressed in synth.xml
        titleBar.setLayout(new BorderLayout());
        final JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setName("TitleBarLabel"); // transparent bg, to be addressed in synth.xml
        titleBar.add(titleLabel, BorderLayout.CENTER);
        if (closeListener != null) {
            //JButton closeButton = new JButton(createIcon(UI.class.getResource("/img/icon/close_button.png"), 18));
            //closeButton.setName("TitleBarCloseButton"); // to be addressed in synth.xml
            //closeButton.addActionListener(closeListener);
            JLabel closeButtonLabel = new JLabel(createIcon(UI.class.getResource("/img/icon/close_button.png"), 18));
            closeButtonLabel.setName("TitleBarLabel"); // transparent bg, to be addressed in synth.xml
            closeButtonLabel.setBorder(new EmptyBorder(3, 0, 3, 6));
            closeButtonLabel.addMouseListener(new DragInsensitiveMouseClickListener(new MouseInputAdapter() {
                public void mouseClicked(MouseEvent e) {
                    closeListener.actionPerformed(new ActionEvent(closeButtonLabel, ActionEvent.ACTION_PERFORMED, ""));
                }
            }));
            titleBar.add(closeButtonLabel, BorderLayout.EAST);
        }
        return titleBar;
    }

    // Convenience methods to display a message from a separate Thread
    public static void alertException(Component parentComponent, String title, String messagePrefix, Exception e) {
        e.printStackTrace();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentComponent, messagePrefix + ":\n" + e.getMessage() + "\nSee console for more information (or start '" + Ginj.getAppName() + " /create-i4j-log' next time to save logs)", title, JOptionPane.ERROR_MESSAGE));
    }

    public static void alertError(Component parentComponent, String title, String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentComponent, message, title, JOptionPane.ERROR_MESSAGE));
    }

    public static void featureNotImplementedDialog(Component parentComponent) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentComponent,
                "This feature is not implemented yet.\n" +
                        "If you think it should have a very high priority,\n" +
                        "feel free to open an issue on Github (or comment on existing one).\n\n" +
                        "Thanks for your patience.", "Feature not implemented",
                JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Returns a panel with the given fields
     * @param keyValues must alternate between String (the label to display) and value (the component for users to enter value)
     * @return
     */
    public static JPanel getFieldPanel(Object ... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new RuntimeException("Field panels must receive an even number of components");
        }

        JPanel fieldsPanel = new JPanel(new BorderLayout());
        JPanel fieldsInnerPanel = new JPanel(new SpringLayout());
        int componentNumber = 0;
        while(componentNumber < keyValues.length) {
            try {
                String key = (String) keyValues[componentNumber];
                JLabel keyLabel = new JLabel(key, JLabel.TRAILING);
                fieldsInnerPanel.add(keyLabel);

                JComponent valueComponent = (JComponent) keyValues[componentNumber + 1];
                keyLabel.setLabelFor(valueComponent);
                fieldsInnerPanel.add(valueComponent);

                keyLabel.setVisible(valueComponent.isVisible());
            }
            catch (ClassCastException e) {
                throw new RuntimeException("Field panels must alternate between JLabel and other components. Received " + keyValues[componentNumber]);
            }
            componentNumber += 2;
        }
        SpringLayoutUtilities.makeCompactGrid(fieldsInnerPanel, keyValues.length/2, 2, 6, 6, 6, 6);

        fieldsPanel.add(fieldsInnerPanel, BorderLayout.NORTH);
        return fieldsPanel;
    }

    // Wizard related utils

    /**
     * Create a new JTextField for use in a Wizard.
     *
     * @param name Will be used to initialize the field value from the map, and later by the wizard to put the field back in the map
     * @param map If the field is present in this map, its value will be used as default value
     * @param defaultText If the field is not present in the map, this will be the default value
     * @param isEnabled if true, the field is enabled
     * @param isVisible if true, the field is visible
     * @return the created JTextField
     */
    public static JTextField createWizardTextField(String name, Map map, String defaultText, boolean isEnabled, boolean isVisible) {
        String text = (String) map.get(name);
        if (text == null) {
            text = defaultText;
        }
        JTextField textField = new JTextField(text, 30);
        textField.setName(name);
        textField.setEnabled(isEnabled);
        textField.setVisible(isVisible);
        return textField;
    }

    /**
     * Create a new JCheckBox for use in a Wizard.
     *
     * @param name Will be used to initialize the field value from the map, and later by the wizard to put the field back in the map
     * @param map If the field is present in this map, its value will be used as default value
     * @param defaultSelected If the field is not present in the map, this will be the default value
     * @param isEnabled if true, the field is enabled
     * @param isVisible if true, the field is visible
     * @return the created JCheckBox
     */
    public static JCheckBox createWizardCheckBox(String name, Map map, boolean defaultSelected, boolean isEnabled, boolean isVisible) {
        Boolean selected = (Boolean) map.get(name);
        if (selected == null) {
            selected = defaultSelected;
        }
        JCheckBox checkBox = new JCheckBox("", selected);
        checkBox.setName(name);
        checkBox.setEnabled(isEnabled);
        checkBox.setVisible(isVisible);
        return checkBox;
    }

    /**
     * Create a new JCheckBox for use in a Wizard.
     * This version is for a "master-detail" presentation, where this "detail" box is only enabled if a "master" box is selected
     *
     * @param name Will be used to initialize the field value from the map, and later by the wizard to put the field back in the map
     * @param map If the field is present in this map, its value will be used as default value
     * @param defaultSelected If the field is not present in the map, this will be the default value
     * @param masterCheckBox this field will be enabled only when the masterCheckbox will be enabled and selected (ticked)
     * @param isVisible if true, the field is visible
     * @return the created JCheckBox
     */
    public static JCheckBox createWizardCheckBox(String name, Map map, boolean defaultSelected, JCheckBox masterCheckBox, boolean isVisible) {
        Boolean selected = (Boolean) map.get(name);
        if (selected == null) {
            selected = defaultSelected;
        }
        JCheckBox checkBox = new JCheckBox("", selected);
        checkBox.setName(name);
        checkBox.setEnabled(masterCheckBox.isSelected());
        checkBox.setVisible(isVisible);
        masterCheckBox.addActionListener(e -> checkBox.setEnabled(masterCheckBox.isSelected() && masterCheckBox.isEnabled()));
        masterCheckBox.addPropertyChangeListener("enabled", e -> checkBox.setEnabled(masterCheckBox.isSelected() && masterCheckBox.isEnabled()));
        return checkBox;
    }

    /**
     * Create a new JList for use in a Wizard.
     *
     * @param name Will be used to initialize the selected value from the map, and later by the wizard to put the selected value back in the map
     * @param values The different values to display in the list
     * @param map If the field is present in this map, its value will be used as default selection
     * @param defaultIndex If the field is not present in the map, this will be the default selected index
     * @param isEnabled if true, the field is enabled
     * @param isVisible if true, the field is visible
     * @return the created JList
     */
    public static <T> JList<T> createWizardList(String name, T[] values, Map map, int defaultIndex, boolean isEnabled, boolean isVisible) {
        final Object object = map.get(name);
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T value: values) {
            model.addElement(value);
        }
        JList<T> list = new JList<>(model);
        list.setName(name);
        if (object != null) {
            list.setSelectedValue(object, true);
        }
        else {
            list.setSelectedIndex(defaultIndex);
        }
        list.setEnabled(isEnabled);
        list.setVisible(isVisible);
        return list;
    }
}
