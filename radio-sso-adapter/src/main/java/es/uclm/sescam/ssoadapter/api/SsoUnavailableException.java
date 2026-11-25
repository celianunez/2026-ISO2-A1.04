package es.uclm.sescam.ssoadapter.api;

import es.uclm.sescam.common.api.DomainException;

/**
 * Thrown when the corporate SSO cannot be reached (or the simulator is in failure mode). The login shows a clear
 * message and the failure is audited; the rest of the system keeps working (D-08). Mapped to HTTP 503 by the login.
 */
public class SsoUnavailableException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param message what failed, for the log and the audit entry
     */
    public SsoUnavailableException(String message) {
        super(message);
    }
}
