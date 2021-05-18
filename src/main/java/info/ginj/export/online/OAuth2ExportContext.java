package info.ginj.export.online;

import com.sun.net.httpserver.HttpServer;
import info.ginj.export.ExportContext;
import info.ginj.export.ExportMonitor;
import info.ginj.ui.StarWindow;

import javax.swing.*;
import java.util.ArrayList;

public class OAuth2ExportContext extends ExportContext {

    private String verifier;
    private String receivedCode = null;
    private ArrayList<String> receivedScopes = null;
    private HttpServer server;

    public OAuth2ExportContext(JFrame parentFrame, StarWindow starWindow, ExportMonitor exportMonitor) {
        super(parentFrame, starWindow, exportMonitor);
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    public String getReceivedCode() {
        return receivedCode;
    }

    public void setReceivedCode(String receivedCode) {
        this.receivedCode = receivedCode;
    }

    public ArrayList<String> getReceivedScopes() {
        return receivedScopes;
    }

    public void setReceivedScopes(ArrayList<String> receivedScopes) {
        this.receivedScopes = receivedScopes;
    }

    public HttpServer getServer() {
        return server;
    }

    public void setServer(HttpServer server) {
        this.server = server;
    }
}
