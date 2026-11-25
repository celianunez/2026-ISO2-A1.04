package es.uclm.sescam.configuration.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when a new value of a system parameter is not valid. Mapped to HTTP 400.
 */
public class InvalidParameterException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param message which value is wrong and why
     */
    public InvalidParameterException(String message) {
        super(message);
    }
}
