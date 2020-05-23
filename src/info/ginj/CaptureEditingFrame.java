package info.ginj;

import info.ginj.export.GinjExporter;
import info.ginj.export.clipboard.ClipboardExporterImpl;
import info.ginj.export.disk.DiskExporterImpl;
import info.ginj.ui.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class CaptureEditingFrame extends JFrame {
    public static final String EXPORT_TYPE_DISK = "disk";
    public static final String EXPORT_TYPE_SHARE = "share";
    public static final String EXPORT_TYPE_CLIPBOARD = "clipboard";

    private BufferedImage capturedImg;
    private String captureId;

    public CaptureEditingFrame(BufferedImage capturedImg) {
        this(capturedImg, new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())); // ENHANCEMENT
    }

    public CaptureEditingFrame(BufferedImage capturedImg, String captureId) {
        super();
        this.capturedImg = capturedImg;
        this.captureId = captureId;
        final Dimension capturedImgSize = new Dimension(capturedImg.getWidth(), capturedImg.getHeight());

        // Make it transparent
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        // Add default "draggable window" behaviour
        addDraggableWindowMouseBehaviour(this);

        final Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        // Prepare title bar
        JPanel titleBar = new JPanel();
        titleBar.setBackground(Color.YELLOW);
        JLabel testLabel = new JLabel("Title");
        titleBar.add(testLabel);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(titleBar, c);

        // Prepare overlay toolbar
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBackground(Util.WINDOW_BACKGROUND_COLOR);

        GinjToolToggleButton arrowToolButton = new GinjToolToggleButton(Util.createIcon(getClass().getResource("img/icon/arrow.png"), 24, 24, Util.TOOLBAR_ICON_ENABLED_COLOR));
        arrowToolButton.setSelected(true);
        toolBar.add(arrowToolButton);
        GinjToolToggleButton textToolButton = new GinjToolToggleButton(Util.createIcon(getClass().getResource("img/icon/text.png"), 24, 24, Util.TOOLBAR_ICON_ENABLED_COLOR));
        toolBar.add(textToolButton);
        GinjToolToggleButton frameToolButton = new GinjToolToggleButton(Util.createIcon(getClass().getResource("img/icon/frame.png"), 24, 24, Util.TOOLBAR_ICON_ENABLED_COLOR));
        toolBar.add(frameToolButton);
        GinjToolToggleButton highlightToolButton = new GinjToolToggleButton(Util.createIcon(getClass().getResource("img/icon/highlight.png"), 24, 24, Util.TOOLBAR_ICON_ENABLED_COLOR));
        toolBar.add(highlightToolButton);
        GinjToolButton colorToolButton = new GinjToolButton(/*new ImageIcon(ImageIO.read(getClass().getResource("img/icon24_color.png")))*/);
        toolBar.add(colorToolButton);

        JPanel undoRedoPanel = new JPanel();
        JButton undoButton = new JButton("U");
        undoRedoPanel.add(undoButton);
        JButton redoButton = new JButton("R");
        undoRedoPanel.add(redoButton);
        toolBar.add(undoRedoPanel);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        contentPane.add(toolBar, c);


        // Prepare main image panel
        JPanel imagePanel = new JPanel() {
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

            @Override
            public Dimension getMinimumSize() {
                return capturedImgSize;
            }

            @Override
            public Dimension getSize(Dimension rv) {
                return capturedImgSize;
            }
        };

        JScrollPane scrollableImagePanel = new JScrollPane(imagePanel);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Util.WINDOW_BACKGROUND_COLOR);
        mainPanel.setLayout(new GridBagLayout());

        c = new GridBagConstraints();
        c.insets = new Insets(13,17,10,17);
        mainPanel.add(scrollableImagePanel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 2;
        contentPane.add(mainPanel, c);


        // Prepare name editing panel
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BorderLayout());
        editPanel.setBackground(Util.LABEL_BACKGROUND_COLOR);
        final JLabel nameLabel = new JLabel("Name");
        nameLabel.setForeground(Util.LABEL_FOREGROUND_COLOR);
        editPanel.add(nameLabel, BorderLayout.WEST);
        JTextField nameTextField = new JTextField();
        nameTextField.setBackground(Util.TEXTFIELD_BACKGROUND_COLOR);
        nameTextField.setSelectionColor(Util.TEXTFIELD_SELECTION_BACKGROUND_COLOR);
        editPanel.add(nameTextField, BorderLayout.CENTER);

        JPanel lowerPanel = new JPanel();
        lowerPanel.setBackground(Util.WINDOW_BACKGROUND_COLOR);
        c = new GridBagConstraints();
        c.insets = new Insets(4,17,12,17);
        lowerPanel.add(editPanel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(lowerPanel, c);


        // Prepare horizontal button bar
        JPanel actionPanel = new JPanel(); // To add a margin around buttonBar
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

        pack();

        // Center window
        setLocationRelativeTo(null);
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
            exporter.export(capturedImg, new Properties());

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
