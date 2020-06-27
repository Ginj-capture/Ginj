package info.ginj.export.online.google;

import info.ginj.Capture;
import info.ginj.Prefs;
import info.ginj.export.online.OnlineService;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;

import javax.swing.*;

public class YoutubeExporter extends GoogleExporter implements OnlineService {

    private static final String[] YOUTUBE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/youtube.upload"};

    public YoutubeExporter(JFrame frame) {
        super(frame);
    }


    @Override
    public void exportCapture(Capture capture, String accountNumber) {

    }


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
