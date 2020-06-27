package info.ginj.export.online.exception;

public abstract class OnlineServiceException extends Exception {
    public OnlineServiceException() {
        super();
    }

    public OnlineServiceException(String message) {
        super(message);
    }

    public OnlineServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnlineServiceException(Throwable cause) {
        super(cause);
    }
}
