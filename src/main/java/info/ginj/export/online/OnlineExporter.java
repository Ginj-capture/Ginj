package info.ginj.export.online;

import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Capture;

public interface OnlineExporter {

    void authorize(String accountNumber) throws AuthorizationException, CommunicationException;

    void checkAuthorizations(String accountNumber) throws CommunicationException, AuthorizationException;

    /**
     * Uploads a capture to the online service and returns its URL
     * @param capture The object representing the captured screenshot or video
     * @param accountNumber The account number among those for this online service
     * @return a public URL to share to give access to the uploaded media, or null if there is no way to share.
     * @throws AuthorizationException
     * @throws UploadException
     * @throws CommunicationException
     */
    String uploadCapture(Capture capture, String accountNumber) throws AuthorizationException, UploadException, CommunicationException;
}