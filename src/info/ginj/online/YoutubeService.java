package info.ginj.online;

import info.ginj.Capture;
import info.ginj.Prefs;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.online.exception.UploadException;

public class YoutubeService extends GoogleService implements OnlineService {

    private static final String[] YOUTUBE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/youtube.upload"};

    @Override
    public String getServiceName() {
        return "Youtube";
    }

    @Override
    protected String[] getRequiredScopes() {
        return YOUTUBE_REQUIRED_SCOPES;
    }

    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_YOUTUBE_REFRESH_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_YOUTUBE_ACCESS_TOKEN_PREFIX;
    }

    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_YOUTUBE_ACCESS_EXPIRY_PREFIX;
    }

    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        // TODO
        return null;
    }
}
