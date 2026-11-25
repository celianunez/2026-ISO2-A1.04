package es.uclm.sescam.audit.api;

/**
 * Result of an audited action.
 */
public enum AuditResult {

    /** The action was done. */
    SUCCESS,

    /** The action was attempted but denied or failed, for example a failed login or an unavailable SSO. */
    FAILURE
}
