package info.ginj.online;

import info.ginj.Capture;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.online.exception.UploadException;

public interface OnlineService {

    public void authorize(String accountNumber) throws AuthorizationException;

    public void abortAuthorization();

    void checkAuthorized(String accountNumber) throws CommunicationException, AuthorizationException;

    public void uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException;
}