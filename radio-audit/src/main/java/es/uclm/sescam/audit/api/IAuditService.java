package es.uclm.sescam.audit.api;

import es.uclm.sescam.common.api.UserRef;
import java.util.List;

/**
 * Public contract of the audit module (CU-19, D-10): the append-only log of who did what on which resource.
 *
 * <p>Every module that reads or changes clinical data records an entry here, and so does every adapter that detects
 * an integration failure (D-08). Entries can never be changed or deleted, so this contract has no update or delete
 * operation. The timestamp of an entry is always taken by the audit module, never by the caller.
 */
public interface IAuditService {

    /** User id stored in an entry whose action had no authenticated user, for example a failed login. */
    String ANONYMOUS_USER_ID = "anonymous";

    /**
     * Records a successful action. Same as {@code record(user, action, resource, AuditResult.SUCCESS)}.
     *
     * @param user     the user who performs the action, or {@code null} when nobody is authenticated
     * @param action   what was done, as a short upper-case verb: {@code LOGIN}, {@code LOGOUT}, {@code READ},
     *                 {@code CREATE}, {@code UPDATE}, {@code DEACTIVATE}, {@code ASSIGN_ROLE}, {@code REMOVE_ROLE}…
     * @param resource what it was done on, as {@code type/id}: {@code patient/P-0001}, {@code catalogue/RX-THORAX},
     *                 {@code auth/login}…
     * @throws IllegalArgumentException if {@code action} or {@code resource} is null or blank
     */
    default void record(UserRef user, String action, String resource) {
        record(user, action, resource, AuditResult.SUCCESS);
    }

    /**
     * Records an action with its result. Use {@link AuditResult#FAILURE} for denied or failed attempts, such as a
     * failed login or an external system that is not available.
     *
     * @param user     the user who performs the action, or {@code null} when nobody is authenticated; the entry then
     *                 stores {@link #ANONYMOUS_USER_ID}
     * @param action   what was done (same format as in {@link #record(UserRef, String, String)})
     * @param resource what it was done on (same format as in {@link #record(UserRef, String, String)})
     * @param result   whether the action succeeded or failed; must not be null
     * @throws IllegalArgumentException if {@code action} or {@code resource} is null or blank, or {@code result} is
     *                                  null
     */
    void record(UserRef user, String action, String resource, AuditResult result);

    /**
     * Returns the entries that match all the given filters, newest first. Only administrators may consult the audit
     * log; the check is done with Spring Security (D-09).
     *
     * @param filters the filters to apply; use {@link AuditQuery#all()} for no filter
     * @return the matching entries, or an empty list if there are none
     * @throws InvalidAuditQueryException if {@code from} is after {@code to}
     */
    List<AuditEntryDto> query(AuditQuery filters);
}
