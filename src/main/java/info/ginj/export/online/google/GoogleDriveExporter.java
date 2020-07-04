package info.ginj.export.online.google;

import info.ginj.export.online.OnlineExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;

import java.util.Arrays;
import java.util.List;

/**
 * See https://developers.google.com/drive/api/v3/manage-uploads#resumable
 * See https://developers.google.com/drive/api/v3/manage-sharing
 */
public class GoogleDriveExporter extends GoogleExporter implements OnlineExporter {
    private static final String[] GOOGLE_DRIVE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/drive"};


    @Override
    public String getExporterName() {
        return "Google Drive";
    }

    @Override
    protected List<String> getRequiredScopes() {
        return Arrays.asList(GOOGLE_DRIVE_REQUIRED_SCOPES);
    }

    @Override
    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_DRIVE_ACCESS_TOKEN_PREFIX;
    }

    @Override
    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_DRIVE_ACCESS_EXPIRY_PREFIX;
    }

    @Override
    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_DRIVE_REFRESH_TOKEN_PREFIX;
    }

    @Override
    public String getShareText() {
        return "Add to Google Drive";
    }

    @Override
    public String getIconPath() {
        return "/img/logo/googledrive.png";
    }

    @Override
    public boolean isImageSupported() {
        return true;
    }

    @Override
    public boolean isVideoSupported() {
        return true;
    }

    /**
     * Uploads the given capture to Google Drive.
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     */
    @Override
    public void exportCapture(Capture capture, String accountNumber) {
        // TODO
    }

    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        // TODO
        return null;
    }
}
