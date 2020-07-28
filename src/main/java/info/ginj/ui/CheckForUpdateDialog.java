package info.ginj.ui;

import info.ginj.Ginj;
import info.ginj.ui.component.DoubleBorderedPanel;
import info.ginj.util.UI;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.IOException;


/**
 * This window displays info about the app
 */
public class CheckForUpdateDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(CheckForUpdateDialog.class);

    public static final String GINJ_UPDATES_XML_URL = "https://github.com/Ginj-capture/Ginj/releases/latest/download/updates.xml";
    public static final String GINJ_RELEASE_PAGE_URL = "https://github.com/Ginj-capture/Ginj/releases";
    private static StarWindow starWindow = null;

    private String latestVersion = null;
    private final JEditorPane editorPane;

    public CheckForUpdateDialog(StarWindow starWindow) {
        super();
        CheckForUpdateDialog.starWindow = starWindow;

        // When entering a modal dialog, hotkey must be disabled, otherwise the app gets locked
        starWindow.unregisterHotKey();
        setModal(true);

        // For Alt+Tab behaviour
        this.setTitle("Check for " + Ginj.getAppName() + " update");
        this.setIconImage(StarWindow.getAppIcon());


        // No window title bar or border.
        // Note: setDefaultLookAndFeelDecorated(true); must not have been called anywhere for this to work
        setUndecorated(true);

        final JPanel contentPane = new DoubleBorderedPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        // Prepare title bar
        JPanel titleBar = UI.getTitleBar("Check for update", e -> onClose());
        contentPane.add(titleBar, BorderLayout.NORTH);


        editorPane = UI.createClickableHtmlEditorPane(getHtmlMessage());
        editorPane.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));
        contentPane.add(editorPane, BorderLayout.CENTER);

        // Prepare lower panel
        JPanel lowerPanel = new JPanel();
        lowerPanel.setOpaque(false);
        lowerPanel.setLayout(new FlowLayout());

        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> onClose());
        lowerPanel.add(closeButton);

        contentPane.add(lowerPanel, BorderLayout.SOUTH);

        // Add default "draggable window" behaviour
        UI.addDraggableWindowMouseBehaviour(this, titleBar);

        //setPreferredSize(WINDOW_PREFERRED_SIZE);
        pack();

        UI.addEscKeyShortcut(this, e -> onClose());

        // Center window
        starWindow.centerFrameOnStarIconDisplay(this);

        startVersionCheckThread();
    }

    private void startVersionCheckThread() {
        new Thread(() -> {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(GINJ_UPDATES_XML_URL);

                //Set the API media type in http accept header
                httpGet.addHeader("accept", "application/xml");

                //Send the request; It will immediately return the response in HttpResponse object
                CloseableHttpResponse response = httpClient.execute(httpGet);

                //verify the valid error code first
                int statusCode = response.getCode();
                if (statusCode == 200) {
                    Document doc = db.parse(response.getEntity().getContent());
                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xp = xpf.newXPath();
                    latestVersion = xp.evaluate("//updateDescriptor/entry[1]/@newVersion", doc.getDocumentElement());
                    editorPane.setText("<html><body>" + getHtmlMessage() + "</body></html>");
                    editorPane.revalidate();
                }
            }
            catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
                UI.alertException(CheckForUpdateDialog.this, "Version check error", "Error retrieving updates.xml file", e, logger);
            }
        }).start();
    }

    private String getHtmlMessage() {
        String message = "Your version: <b><span style=\"color:#00FF00\">" + Ginj.getVersion() + "</span></b><br/>";

        if (latestVersion == null) {
            message += "Fetching latest version...<br/>" +
                    "For details about " + Ginj.getAppName() + " releases, take a look at the";
         }
        else {
            int compared = latestVersion.compareTo(Ginj.getVersion());
            if (compared == 0) {
                message += "Your version is up to date.<br/>" +
                        "For details about " + Ginj.getAppName() + " releases, take a look at the";
            }
            else if (compared < 0) {
                message += "Latest version: <b><span style=\"color:#00FFFF\">" + latestVersion + "</span></b><br/>" +
                        "You seem to be running a pre-release version.<br/>" +
                        "For details about all releases, please see the";
            }
            else {
                message += "Latest version: <b><span style=\"color:#FF0000\">" + latestVersion + "</span></b><br/>" +
                        "For details about that release<br/>" +
                        "and download links, please see the";
            }
        }

        message += "<br/><a href=\"" + GINJ_RELEASE_PAGE_URL + "\">Github releases</a> page for the project.";

        return message;
    }

    private void onClose() {
        // Restore the previous hotkey
        starWindow.registerHotKey();
        // Close window
        dispose();
    }
}
