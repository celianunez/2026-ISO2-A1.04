package es.uclm.sescam.catalogue.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when a mandatory attribute of a test type is missing or a value is not valid. Mapped to HTTP 400.
 */
public class InvalidTestTypeException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param message which attribute is wrong and why
     */
    public InvalidTestTypeException(String message) {
        super(message);
    }
}
