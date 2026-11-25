package es.uclm.sescam.ssoadapter.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when the corporate SSO does not recognise a token, or the token has expired.
 */
public class InvalidSsoTokenException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param message why the token was rejected (never include the token itself)
     */
    public InvalidSsoTokenException(String message) {
        super(message);
    }
}
