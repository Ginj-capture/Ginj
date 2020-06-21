package info.ginj.online.exception;

public abstract class OnlineServiceExption extends Exception {
    public OnlineServiceExption() {
        super();
    }

    public OnlineServiceExption(String message) {
        super(message);
    }

    public OnlineServiceExption(String message, Throwable cause) {
        super(message, cause);
    }

    public OnlineServiceExption(Throwable cause) {
        super(cause);
    }
}
