package info.ginj.export.online;

import info.ginj.Capture;
import info.ginj.export.GinjExporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.ui.Util;

import javax.swing.*;

public abstract class AbstractOnlineExporter extends GinjExporter implements OnlineService {
    public AbstractOnlineExporter(JFrame frame) {
        super(frame);
    }

    @Override
    public boolean prepare(Capture capture, String accountNumber) {
        try {
            checkAuthorizations(accountNumber);
        }
        catch (AuthorizationException e) {
            // TODO should we just show an error here ? Authorization should be done in account management
            try {
                authorize(accountNumber);
            }
            catch (AuthorizationException authorizationException) {
                Util.alertException(getFrame(), getServiceName() + "authorization error", "There was an error authorizing you on " + getServiceName(), e);
                failed("Authorization error");
                return false;
            }
        }
        catch (CommunicationException e) {
            Util.alertException(getFrame(), getServiceName() + "authorization check error", "There was an error checking authorization on " + getServiceName(), e);
            failed("Communication error");
            return false;
        }
        return true;
    }


}
