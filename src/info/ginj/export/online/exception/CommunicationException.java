package info.ginj.export.online.exception;

public class CommunicationException extends OnlineServiceException {
    public CommunicationException() {
        super();
    }

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationException(Throwable cause) {
        super(cause);
    }
}
