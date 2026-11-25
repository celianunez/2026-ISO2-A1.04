package es.uclm.sescam.common.api;

/**
 * The four roles of the system (HU-18, P-07).
 *
 * <p>Every authenticated user has at least one of them. They are checked on the server with
 * Spring Security ({@code @PreAuthorize("hasRole('...')")}), never only in the UI.
 */
public enum Role {

    /** Patient: sees only their own requests, appointments and reports. */
    PATIENT,

    /** Physician: prescribes tests, records executions and signs reports. */
    PHYSICIAN,

    /** Admissions staff: registers patients and books appointments. */
    ADMISSIONS,

    /** Administrator: manages the catalogue, the parameters, the roles and the audit. */
    ADMINISTRATOR;

    /**
     * The name this role has in a Spring Security authority ({@code ROLE_PHYSICIAN}).
     *
     * @return the authority string expected by {@code hasRole(...)}
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
