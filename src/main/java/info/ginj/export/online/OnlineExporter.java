package info.ginj.export.online;

import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Account;
import info.ginj.model.Capture;
import info.ginj.model.Target;

public interface OnlineExporter {

    Account authorize() throws AuthorizationException, CommunicationException;

    /**
     * This method checks that the account is valid and authorized
     *
     * @param account the account to validate
     * @throws CommunicationException in case a communication error occurs
     * @throws AuthorizationException in case authorization fails
     */
    void checkAuthorizations(Account account) throws CommunicationException, AuthorizationException;

    /**
     * Uploads a capture to the online service and returns its URL
     * @param capture The object representing the captured screenshot or video
     * @param target  the target to export this capture to
     * @return a public URL to share to give access to the uploaded media, or null if there is no way to share.
     * @throws AuthorizationException
     * @throws UploadException
     * @throws CommunicationException
     */
    String uploadCapture(Capture capture, Target target) throws AuthorizationException, UploadException, CommunicationException;
}