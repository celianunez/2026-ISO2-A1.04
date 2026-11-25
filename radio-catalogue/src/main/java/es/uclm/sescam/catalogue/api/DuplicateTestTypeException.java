package es.uclm.sescam.catalogue.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when a test type is created with a code that already exists. Mapped to HTTP 400.
 */
public class DuplicateTestTypeException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param code the repeated code
     */
    public DuplicateTestTypeException(String code) {
        super("A test type with code " + code + " already exists");
    }
}
