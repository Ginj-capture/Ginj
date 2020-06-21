package info.ginj.online;

import info.ginj.Capture;
import info.ginj.Prefs;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.online.exception.UploadException;

public class GoogleDriveService extends GoogleService implements OnlineService {

    private static final String[] GOOGLE_DRIVE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/drive"};

    @Override
    protected String getServiceName() {
        return "Google Drive";
    }

    @Override
    protected String[] getRequiredScopes() {
        return GOOGLE_DRIVE_REQUIRED_SCOPES;
    }

    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_DRIVE_REFRESH_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_DRIVE_ACCESS_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_GOOGLE_DRIVE_ACCESS_EXPIRY_PREFIX;
    }

    @Override
    public void checkAuthorized(String accountNumber) throws CommunicationException, AuthorizationException {
        // TODO
    }

    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        // TODO
        return null;
    }
}
