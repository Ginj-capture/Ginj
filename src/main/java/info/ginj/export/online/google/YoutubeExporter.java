package info.ginj.export.online.google;

import info.ginj.export.online.OnlineExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Capture;
import info.ginj.model.Prefs;

import java.util.Arrays;
import java.util.List;

public class YoutubeExporter extends GoogleExporter implements OnlineExporter {

    private static final String[] YOUTUBE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/youtube.upload"};


    @Override
    public String getExporterName() {
        return "Youtube";
    }

    @Override
    protected List<String> getRequiredScopes() {
        return Arrays.asList(YOUTUBE_REQUIRED_SCOPES);
    }

    @Override
    protected Prefs.Key getAccessTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_YOUTUBE_ACCESS_TOKEN_PREFIX;
    }

    @Override
    protected Prefs.Key getAccessExpiryKeyPrefix() {
        return Prefs.Key.EXPORTER_YOUTUBE_ACCESS_EXPIRY_PREFIX;
    }

    @Override
    protected Prefs.Key getRefreshTokenKeyPrefix() {
        return Prefs.Key.EXPORTER_YOUTUBE_REFRESH_TOKEN_PREFIX;
    }

    @Override
    public String getShareText() {
        return "Publish on Youtube";
    }

    @Override
    public String getIconPath() {
        return "/img/logo/youtube.png";
    }

    @Override
    public boolean isImageSupported() {
        return false;
    }

    @Override
    public boolean isVideoSupported() {
        return true;
    }

    @Override
    public void exportCapture(Capture capture, String accountNumber) {

    }

    @Override
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException {
        // TODO
        return null;
    }
}
