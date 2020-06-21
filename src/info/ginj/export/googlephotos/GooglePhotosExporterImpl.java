package info.ginj.export.googlephotos;

import info.ginj.Capture;
import info.ginj.export.GinjExporter;
import info.ginj.online.GooglePhotosService;
import info.ginj.online.OnlineService;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.ui.Util;

import javax.swing.*;

public class GooglePhotosExporterImpl extends GinjExporter {
    public GooglePhotosExporterImpl(JFrame frame) {
        super(frame);
    }

    /**
     * Exports the given capture
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to
     * @return true if export completed, or false otherwise
     */
    @Override
    public boolean exportCapture(Capture capture, String accountNumber) {
        final OnlineService service = new GooglePhotosService();

        try {
            service.checkAuthorizations(accountNumber);
        }
        catch (AuthorizationException e) {
            try {
                service.authorize(accountNumber);
            }
            catch (AuthorizationException authorizationException) {
                Util.alertException(getFrame(), service.getServiceName() + "authorization error", "There was an error authorizing you on " + service.getServiceName(), e);
            }
        }
        catch (CommunicationException e) {
            Util.alertException(getFrame(), service.getServiceName() + "authorization check error", "There was an error checking authorization on " + service.getServiceName(), e);
        }

        try {
            final String albumUrl = service.uploadCapture(capture, accountNumber);
            if (albumUrl != null) {
                copyTextToClipboard(albumUrl);
                // Show message "complete"
            }
        }
        catch (Exception e) {
            Util.alertException(getFrame(), service.getServiceName() + "Error", "There was an error exporting to " + service.getServiceName(), e);
            return false;
        }
        return true;
    }
}
