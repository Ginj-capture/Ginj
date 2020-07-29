package info.ginj.export.online;

import info.ginj.export.Exporter;
import info.ginj.export.online.exception.AuthorizationException;
import info.ginj.export.online.exception.CommunicationException;
import info.ginj.export.online.exception.UploadException;
import info.ginj.model.Account;
import info.ginj.model.Capture;
import info.ginj.model.Export;
import info.ginj.model.Target;

public abstract class AbstractOnlineExporter extends Exporter {

    public int PROGRESS_CHECK_AUTHORIZE_START = 2;
    public int PROGRESS_CREATING_ALBUM = 5;
    public int PROGRESS_GETTING_ALBUM = 6;
    public int PROGRESS_SHARING_ALBUM = 8;
    public int PROGRESS_RENDER_START = 10;
    public int PROGRESS_UPLOAD_START = 20;
    public int PROGRESS_UPLOAD_END = 90;
    public int PROGRESS_CREATING_MEDIA = 95;

    /** The chunk size to use for data upload.
     * Must be a multiple of 256*1024 for Google Drive
     * TODO make it larger for better performance
     */
    public static final int CHUNK_SIZE = 256*1024;

    public abstract Account authorize() throws AuthorizationException, CommunicationException;

    /**
     * This method checks that the account is valid and authorized
     *
     * @param account the account to validate
     * @throws CommunicationException in case a communication error occurs
     * @throws AuthorizationException in case authorization fails
     */
    public abstract void checkAuthorizations(Account account) throws CommunicationException, AuthorizationException;

    /**
     * Uploads a capture to the online service and returns its URL
     * @param capture The object representing the captured screenshot or video
     * @param target  the target to export this capture to
     * @return a public URL to share to give access to the uploaded media, or null if there is no way to share.
     * @throws AuthorizationException in case authorization fails
     * @throws UploadException        if an upload-specific error occurs
     * @throws CommunicationException in case a communication error occurs
     */
    public abstract Export uploadCapture(Capture capture, Target target) throws AuthorizationException, UploadException, CommunicationException;
}