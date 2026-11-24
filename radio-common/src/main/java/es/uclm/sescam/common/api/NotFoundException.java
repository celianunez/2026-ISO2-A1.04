package es.uclm.sescam.common.api;

/**
 * The requested resource does not exist. Mapped to HTTP 404 by {@code radio-app}.
 */
public class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what was not found, for example {@code "Test type 42 not found"}
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * @param message what was not found
     * @param cause the underlying error, kept for the log
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
