package es.uclm.sescam.audit.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when the filters of an audit query are not valid, for example when {@code from} is after {@code to}.
 * Mapped to HTTP 400.
 */
public class InvalidAuditQueryException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param message what is wrong with the filters
     */
    public InvalidAuditQueryException(String message) {
        super(message);
    }
}
