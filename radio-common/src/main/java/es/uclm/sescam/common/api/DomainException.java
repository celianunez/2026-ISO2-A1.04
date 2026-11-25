package es.uclm.sescam.common.api;

/**
 * Base class of every business error of the domain (D-20).
 *
 * <p>Unchecked, so it does not force a {@code throws} clause through the layers. It is mapped to
 * HTTP 400 by the error handler of {@code radio-app} unless a subclass says otherwise
 * ({@link NotFoundException} → 404, {@link ForbiddenOperationException} → 403).
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message human readable explanation, shown to the client and written in the log
     */
    public DomainException(String message) {
        super(message);
    }

    /**
     * @param message human readable explanation
     * @param cause the underlying error, kept for the log
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
