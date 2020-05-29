package info.ginj;

import info.ginj.export.GinjExporter;
import info.ginj.export.clipboard.ClipboardExporterImpl;
import info.ginj.export.disk.DiskExporterImpl;
import info.ginj.tool.GinjTool;
import info.ginj.tool.Overlay;
import info.ginj.tool.arrow.ArrowTool;
import info.ginj.tool.frame.FrameTool;
import info.ginj.tool.highlight.HighlightTool;
import info.ginj.tool.text.TextTool;
import info.ginj.ui.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class CaptureEditingFrame extends JFrame {
    public static final String EXPORT_TYPE_DISK = "disk";
    public static final String EXPORT_TYPE_SHARE = "share";
    public static final String EXPORT_TYPE_CLIPBOARD = "clipboard";

    public static final int TOOL_BUTTON_ICON_WIDTH = 24;
    public static final int TOOL_BUTTON_ICON_HEIGHT = 24;
    public static final int MINI_TOOL_BUTTON_ICON_WIDTH = 10;
    public static final int MINI_TOOL_BUTTON_ICON_HEIGHT = 10;
    public static final Insets MAIN_PANEL_INSETS = new Insets(13, 17, 10, 17);

    // Caching
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final Dimension capturedImgSize;

    // State
    private final BufferedImage capturedImg;
    private final String captureId;
    private final java.util.List<Overlay> overLays = new ArrayList<>();
    private final JLayeredPane imagePane;

    private GinjTool currentTool;
    private Color currentColor = Color.RED;
    private int currentDepth = 0;


    public CaptureEditingFrame(BufferedImage capturedImg) {
        this(capturedImg, new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())); // ENHANCEMENT: seconds
    }

    public CaptureEditingFrame(BufferedImage capturedImg, String captureId) {
        super();
        this.capturedImg = capturedImg;
        this.captureId = captureId;
        capturedImgSize = new Dimension(capturedImg.getWidth(), capturedImg.getHeight());

        // For Alt+Tab behaviour
        this.setTitle(Ginj.getAppName() + " Preview");
        // this.setIconImage(); TODO

        // Make it transparent
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        // Add default "draggable window" behaviour
        addDraggableWindowMouseBehaviour(this);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints c;

        // Prepare title bar
        JPanel titleBar = new JPanel();
        titleBar.setBackground(Color.YELLOW);
        JLabel testLabel = new JLabel("Title");
        titleBar.add(testLabel);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(titleBar, c);

        // Prepare overlay toolbar
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBorder(new EmptyBorder(6,6,6,6));
        toolBar.setBackground(Util.WINDOW_BACKGROUND_COLOR);

        ButtonGroup toolButtonGroup = new ButtonGroup();
        GinjTool[] tools = new GinjTool[] {new ArrowTool(), new TextTool(), new FrameTool(), new HighlightTool()};
        for (GinjTool tool : tools) {
            addToolButton(toolBar, tool, toolButtonGroup);
        }

        GinjToolButton colorToolButton = new GinjToolButton(createRoundRectColorIcon(currentColor, TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT));
        colorToolButton.addActionListener(e -> {
            if (Color.RED.equals(currentColor)) {
                currentColor = Color.GREEN;
            }
            else if (Color.GREEN.equals(currentColor)) {
                currentColor = Color.BLUE;
            }
            else {
                currentColor = Color.RED;
            }
            colorToolButton.setIcon(createRoundRectColorIcon(currentColor, TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT));
        });
        colorToolButton.setToolTipText("Tool Color");
        toolBar.add(colorToolButton);
        toolBar.add(Box.createRigidArea(new Dimension(0, 8)));

        // TODO implement undo
        JPanel undoRedoPanel = new JPanel();
        undoRedoPanel.setAlignmentX(0); // Otherwise the panel adds horizontal space...
        undoRedoPanel.setLayout(new BoxLayout(undoRedoPanel, BoxLayout.X_AXIS));
        GinjMiniToolButton undoButton = new GinjMiniToolButton(Util.createIcon(getClass().getResource("img/icon/undo.png"), MINI_TOOL_BUTTON_ICON_WIDTH, MINI_TOOL_BUTTON_ICON_HEIGHT, Util.TOOLBAR_ICON_ENABLED_COLOR));
        undoButton.setEnabled(false);
        undoButton.setToolTipText("Undo");
        undoRedoPanel.add(undoButton);
        GinjMiniToolButton redoButton = new GinjMiniToolButton(Util.createIcon(getClass().getResource("img/icon/redo.png"), MINI_TOOL_BUTTON_ICON_WIDTH, MINI_TOOL_BUTTON_ICON_HEIGHT, Util.TOOLBAR_ICON_ENABLED_COLOR));
        redoButton.setToolTipText("Redo");
        redoButton.setEnabled(false);
        undoRedoPanel.add(redoButton);
        toolBar.add(undoRedoPanel);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        contentPane.add(toolBar, c);


        // Prepare main image panel
        imagePane = new JLayeredPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.drawImage(capturedImg, 0, 0, this);
            }

            @Override
            public Dimension getPreferredSize() {
                return capturedImgSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return capturedImgSize;
            }

        };
        // Absolute positioning of components over the image
        imagePane.setLayout(null);

        addMouseEditingBehaviour(imagePane);

        // Prepare an opaque panel which will fill the main display area and host the image scrollpane
        // (the scrollpane will only occupy the center if image is smaller than the toolbars)
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(true);
        mainPanel.setBackground(Util.WINDOW_BACKGROUND_COLOR);
        mainPanel.setLayout(new GridBagLayout());

        // Let's be confident that the image will fit and we won't need a scrollpane
        // (that will be checked at the end and fixed if needed)
        // Not using a scrollpane guarantees the image will be fully visible, centered, if it is smaller than the available display area
        c = new GridBagConstraints();
        // min border around scrollPane
        c.insets = MAIN_PANEL_INSETS;
        c.weightx = 1;
        c.weighty = 1;
        mainPanel.add(imagePane, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(mainPanel, c);


        // Prepare name editing panel
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BorderLayout());
        editPanel.setBackground(Util.LABEL_BACKGROUND_COLOR);
        final JLabel nameLabel = new JLabel("Name ");
        nameLabel.setForeground(Util.LABEL_FOREGROUND_COLOR);
        editPanel.add(nameLabel, BorderLayout.WEST);
        JTextField nameTextField = new JTextField();
        nameTextField.setBackground(Util.TEXTFIELD_BACKGROUND_COLOR);
        nameTextField.setSelectionColor(Util.TEXTFIELD_SELECTION_BACKGROUND_COLOR);
        nameTextField.setSelectedTextColor(Util.TEXTFIELD_SELECTION_FOREGROUND_COLOR);
        editPanel.add(nameTextField, BorderLayout.CENTER);

        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new GridBagLayout());
        lowerPanel.setBackground(Util.WINDOW_BACKGROUND_COLOR);
        c = new GridBagConstraints();
        c.insets = new Insets(4,17,12,17);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        lowerPanel.add(editPanel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(lowerPanel, c);


        // Prepare horizontal button bar
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actionPanel.setName("GinjPanel"); // To be used as a selector in laf.xml
        JPanel buttonBar = new GinjButtonBar();

        GinjButton shareButton = new GinjButton("Share via X", Util.createIcon(getClass().getResource("img/icon/share.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        shareButton.addActionListener(e -> onExport(EXPORT_TYPE_SHARE));
        buttonBar.add(shareButton);
        GinjButton saveButton = new GinjButton("Save", Util.createIcon(getClass().getResource("img/icon/save.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        saveButton.addActionListener(e -> onExport(EXPORT_TYPE_DISK));
        buttonBar.add(saveButton);
        final JButton copyButton = new GinjButton("Copy", Util.createIcon(getClass().getResource("img/icon/copy.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        copyButton.addActionListener(e -> onExport(EXPORT_TYPE_CLIPBOARD));
        buttonBar.add(copyButton);
        final JButton cancelButton = new GinjButton("Cancel", Util.createIcon(getClass().getResource("img/icon/cancel.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        cancelButton.addActionListener(e -> onCancel());
        buttonBar.add(cancelButton);
        final JButton customizeButton = new GinjButton("Customize Ginj buttons", Util.createIcon(getClass().getResource("img/icon/customize.png"), 16, 16, Util.ICON_ENABLED_COLOR));
        customizeButton.addActionListener(e -> onCustomize());
        buttonBar.add(customizeButton);

        actionPanel.add(buttonBar);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        contentPane.add(actionPanel, c);

        // Prefill and select name
        // TODO does not work
        nameTextField.setText(captureId);
        nameTextField.requestFocusInWindow();
        nameTextField.selectAll();

        /////////////////////////////
        // Check that the window fits on screen, or adjust size and add a scrollpane if needed

        // Lay out components
        pack();

        // Make sure the window fits on the screen:

        // Find the usable screen size (without the Taskbar)
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = env.getMaximumWindowBounds();

        // And compare with current size, choosing the smallest one
        boolean mustResize = false;
        Dimension size = getSize();
        if (size.width > bounds.width) {
            // Must limit width
            size.width = bounds.width;
            mustResize = true;
        }
        if (size.height > bounds.height) {
            // Must limit height
            size.height = bounds.height;
            mustResize = true;
        }

        // See if a resize is needed or not
        if (mustResize) {
            // Replace the imagePane by a JScrollPane filling the whole space and containing in turn the imagePane
            c = new GridBagConstraints();
            // min border around scrollPane
            c.insets = MAIN_PANEL_INSETS;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            JScrollPane scrollableImagePanel = new JScrollPane(imagePane);
            mainPanel.add(scrollableImagePanel, c);
        }
        // Lay out components again
        pack();
        // And limit size
        setSize(size);

        // Center window
        setLocationRelativeTo(null);
    }

    private void addToolButton(JPanel toolBar, GinjTool tool, ButtonGroup group) {
        // Create button
        GinjToolToggleButton toolButton = new GinjToolToggleButton(Util.createIcon(
                getClass().getResource("img/icon/tool_" + tool.getName().toLowerCase() + ".png"),
                TOOL_BUTTON_ICON_WIDTH, TOOL_BUTTON_ICON_HEIGHT, Util.TOOLBAR_ICON_ENABLED_COLOR));
        // Add it to the toolbar, followed by a spacer
        toolButton.setToolTipText(tool.getName());
        toolBar.add(toolButton);
        toolBar.add(Box.createRigidArea(new Dimension(0, 8)));

        // Add the button to the buttonGroup to get a "radio-button" experience
        group.add(toolButton);

        // Select this button if it's the first
        if (currentTool == null) {
            toolButton.setSelected(true);
            currentTool = tool;
        }

        // And store this tool as currentTool if clicked
        toolButton.addActionListener((event) -> {
            currentTool = tool;
        });
    }

    private Icon createRoundRectColorIcon(Color color, int width, int height) {
        return new Icon(){
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(x, y, width, height, width/3, height/3);
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

    /*
     * TODO add click filter like on star window
     */
    private void addMouseEditingBehaviour(JLayeredPane panel) {
        MouseInputListener mouseListener = new MouseInputAdapter() {
            private int selectedHandle = 0;
            private Overlay selectedOverlay;
            Point clicked;

            public void mousePressed(MouseEvent e) {
                clicked = e.getPoint();

                // See if we clicked in a component
                boolean found = false;
                for (Overlay overLay : overLays) {
                    overLay.setSelected(false);
                    if (!found) {
                        if (overLay.isInComponent(clicked)) {
                            overLay.setSelected(true);
                            found = true;
                        }
                    }
                }
                if (!found) {
                    // No component selected. Create a new one
                    selectedOverlay = currentTool.createComponent(clicked, currentColor);
                    currentDepth++;
                    panel.add(selectedOverlay, Integer.valueOf(currentDepth));
                    selectedOverlay.setBounds(0, 0, capturedImgSize.width, capturedImgSize.height);
                    overLays.add(0, selectedOverlay); // At it to the beginning, so that it it tested first in case of click
                    selectedOverlay.setSelected(true);
                    selectedHandle = 0;
                }
                // TODO Remember the "before" state to be able to undo
                repaint();
            }

            public void mouseDragged(MouseEvent e) {
                selectedOverlay.moveHandle(0, e.getPoint());
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                if (selectedOverlay.hasNoSize()) {
                    panel.remove(selectedOverlay);
                    overLays.remove(selectedOverlay);
                }
                else {
                    // TODO Add edit (creation, selection, modification) to undo stack
                }
            }
        };
        panel.addMouseListener(mouseListener);
        panel.addMouseMotionListener(mouseListener);
    }


    private void addDraggableWindowMouseBehaviour(CaptureEditingFrame frame) {
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
        frame.addMouseListener(mouseListener);
        frame.addMouseMotionListener(mouseListener);
    }


    private void onExport(String exportType) {
        // Always store in history, no matter the export type
        saveInHistory();

        // Render image and overlays
        BufferedImage renderedImage = new BufferedImage(imagePane.getWidth(), imagePane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = renderedImage.getGraphics();
        imagePane.paint(g);
        g.dispose();

        // Find the right exporter implementation
        GinjExporter exporter = null;
        switch (exportType) {
            case EXPORT_TYPE_SHARE:
                //exporter = new ShareExporterImpl(this);
                break;
            case EXPORT_TYPE_DISK:
                exporter = new DiskExporterImpl(this);
                break;
            case EXPORT_TYPE_CLIPBOARD:
                exporter = new ClipboardExporterImpl(this);
                break;
        }

        // Perform export
        if (exporter != null) {
            exporter.export(renderedImage, new Properties());

            // and close Window
            dispose();
        }
        else {
            JOptionPane.showMessageDialog(this, "Cannot find an exporter for type '" + exportType + "'.", "Export error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveInHistory() {
        File historyFolder = new File("ZZhistoryFolder"); // TODO get from params
        if (!historyFolder.exists()) {
            historyFolder.mkdirs();
        }
        // Save image
        File file = new File(historyFolder, captureId + ".png");
        try {
            if (!ImageIO.write(capturedImg, "png", file)) {
                JOptionPane.showMessageDialog(this, "Capture failed (" + file.getAbsolutePath() + ")", "Screen capture error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage() + " - Full error on the console", "Screen capture error", JOptionPane.ERROR_MESSAGE);
        }

        // TODO save overlays to XML

    }

    private void onCancel() {
        // Close window
        dispose();
    }

    private void onCustomize() {
        // TODO
    }

}
