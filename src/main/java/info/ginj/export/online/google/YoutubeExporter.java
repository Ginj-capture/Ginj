package info.ginj.export.online.google;

import info.ginj.export.online.OnlineExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Capture;
import info.ginj.model.Target;

import java.util.Arrays;
import java.util.List;

public class YoutubeExporter extends GoogleExporter implements OnlineExporter {

    private static final String[] YOUTUBE_REQUIRED_SCOPES = {"https://www.googleapis.com/auth/youtube.upload"};
    public static final String NAME = "Youtube";


    @Override
    public String getExporterName() {
        return NAME;
    }

    @Override
    protected List<String> getRequiredScopes() {
        return Arrays.asList(YOUTUBE_REQUIRED_SCOPES);
    }

    @Override
    public String getDefaultShareText() {
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
    public void exportCapture(Capture capture, Target target) {
    }

    @Override
    public String uploadCapture(Capture capture, Target target) throws AuthorizationException, UploadException, CommunicationException {
        // TODO
        return null;
    }
}
