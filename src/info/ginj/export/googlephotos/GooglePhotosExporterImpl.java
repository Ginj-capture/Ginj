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

    @Override
    public boolean prepare(Capture capture, String accountNumber) {
        final OnlineService service = new GooglePhotosService();
        service.setExportMonitor(getExportMonitor());

        try {
            service.checkAuthorizations(accountNumber);
        }
        catch (AuthorizationException e) {
            // TODO should we just show an error here ? Authorization should be done in account management
            try {
                service.authorize(accountNumber);
            }
            catch (AuthorizationException authorizationException) {
                Util.alertException(getFrame(), service.getServiceName() + "authorization error", "There was an error authorizing you on " + service.getServiceName(), e);
                failed("Authorization error");
                return false;
            }
        }
        catch (CommunicationException e) {
            Util.alertException(getFrame(), service.getServiceName() + "authorization check error", "There was an error checking authorization on " + service.getServiceName(), e);
            failed("Communication error");
            return false;
        }
        return true;
    }

    /**
     * Uploads the given capture to Google Photos
     * This method is run in its own thread and should not access the GUI directly. All interaction
     * should go through synchronized objects or be enclosed in a SwingUtilities.invokeLater() logic
     *
     * @param capture       the capture to export
     * @param accountNumber the accountNumber to export this capture to (if relevant)
     */
    @Override
    public void exportCapture(Capture capture, String accountNumber) {
        final OnlineService service = new GooglePhotosService();
        service.setExportMonitor(getExportMonitor());

        try {
            final String albumUrl = service.uploadCapture(capture, accountNumber);
            if (albumUrl != null) {
                copyTextToClipboard(albumUrl);
                // Indicate export is complete.
                complete("Upload successful. A link to the album containing your capture was copied to the clipboard");
            }
        }
        catch (Exception e) {
            Util.alertException(getFrame(), service.getServiceName() + "Error", "There was an error exporting to " + service.getServiceName(), e);
        }
    }
}
