package info.ginj.export.googlephotos;

import info.ginj.Capture;
import info.ginj.export.GinjExporter;
import info.ginj.online.GooglePhotosService;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.ui.Util;

import javax.swing.*;

/**
 */
public class GooglePhotosExporterImpl extends GinjExporter {
    public GooglePhotosExporterImpl(JFrame frame) {
        super(frame);
    }

    /**
     * Exports the given capture
     *
     * @param capture        the capture to export
     * @param accountNumber  the accountNumber to export this capture to
     * @return true if export completed, or false otherwise
     */
    @Override
    public boolean exportCapture(Capture capture, String accountNumber) {
        final GooglePhotosService googlePhotosService = new GooglePhotosService();

        try {
            googlePhotosService.checkAuthorized(accountNumber);
        }
        catch (AuthorizationException e) {
            try {
                googlePhotosService.authorize(accountNumber);
            }
            catch (AuthorizationException authorizationException) {
                Util.alertException(getFrame(), "Google Photo authorization error", "There was an error authorizing you on Google Photos", e);
            }
        }
        catch (CommunicationException e) {
            Util.alertException(getFrame(), "Google Photo authorization check error", "There was an error checking authorization on Google Photos", e);
        }

        try {
            googlePhotosService.uploadCapture(capture, accountNumber);
        }
        catch (Exception e) {
            Util.alertException(getFrame(), "Google Photo Error", "There was an error exporting to Google Photos", e);
            return false;
        }
        return true;
    }
}
