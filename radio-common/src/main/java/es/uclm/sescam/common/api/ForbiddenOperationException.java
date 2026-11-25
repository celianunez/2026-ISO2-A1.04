package es.uclm.sescam.common.api;

/**
 * The authenticated user is not allowed to perform the operation (HU-18).
 *
 * <p>Mapped to HTTP 403 by {@code radio-app}. Permissions are always checked on the server;
 * hiding the button in the UI is only a courtesy.
 */
public class ForbiddenOperationException extends DomainException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what the user is not allowed to do, for example
     *     {@code "Only an administrator may deactivate a test type"}
     */
    public ForbiddenOperationException(String message) {
        super(message);
    }

    /**
     * @param message what the user is not allowed to do
     * @param cause the underlying error, kept for the log
     */
    public ForbiddenOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
