package es.uclm.sescam.auth.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when a login is rejected. The reason tells the web layer which answer to give: {@link Reason#INVALID_TOKEN}
 * → HTTP 401, {@link Reason#NO_ROLE} → HTTP 403.
 */
public class AuthenticationFailedException extends DomainException {

    /** Why a login was rejected. */
    public enum Reason {

        /** The SSO does not recognise the token, or the token has expired. */
        INVALID_TOKEN,

        /** The corporate identity is valid but has no role in this application (CU-20 criterion 3). */
        NO_ROLE
    }

    private final Reason reason;

    /**
     * Creates the exception.
     *
     * @param reason  why the login was rejected; must not be null
     * @param message text for the log and the audit entry (never include the token)
     */
    public AuthenticationFailedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * Returns why the login was rejected.
     *
     * @return the reason
     */
    public Reason reason() {
        return reason;
    }
}
