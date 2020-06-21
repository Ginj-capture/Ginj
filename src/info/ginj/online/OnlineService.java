package info.ginj.online;

import info.ginj.Capture;
import info.ginj.online.exception.AuthorizationException;
import info.ginj.online.exception.CommunicationException;
import info.ginj.online.exception.UploadException;

public interface OnlineService {

    public void authorize(String accountNumber) throws AuthorizationException;

    public void abortAuthorization();

    void checkAuthorized(String accountNumber) throws CommunicationException, AuthorizationException;

    /**
     * Upload a capture to the online service and return its URL
     * @param capture The captured screenshot or video
     * @param accountNumber The account number among those for this online service
     * @return a public URL to share to give access to the uploaded media, or null if there is no way to share.
     * @throws AuthorizationException
     * @throws UploadException
     * @throws CommunicationException
     */
    public String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException;
}