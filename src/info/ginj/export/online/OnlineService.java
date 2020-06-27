package info.ginj.export.online;

import info.ginj.Capture;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;

public interface OnlineService {

    String getServiceName();

    void authorize(String accountNumber) throws AuthorizationException;

    void abortAuthorization();

    void checkAuthorizations(String accountNumber) throws CommunicationException, AuthorizationException;

    /**
     * Upload a capture to the online service and return its URL
     * @param capture The object representing the captured screenshot or video
     * @param accountNumber The account number among those for this online service
     * @return a public URL to share to give access to the uploaded media, or null if there is no way to share.
     * @throws AuthorizationException
     * @throws UploadException
     * @throws CommunicationException
     */
    String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException;
}