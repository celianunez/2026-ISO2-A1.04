package es.uclm.sescam.audit.api;

import es.uclm.sescam.common.api.Role;
import java.time.Instant;
import java.util.Optional;

/**
 * One entry of the audit log, as returned by {@link IAuditService#query(AuditQuery)}. Entries are immutable.
 *
 * @param id        identifier of the entry, assigned by the audit module
 * @param timestamp when the action happened, taken from the clock of the audit module
 * @param userId    id of the user who did the action, or {@link IAuditService#ANONYMOUS_USER_ID} if nobody was
 *                  authenticated
 * @param role      role the user acted with; empty if nobody was authenticated
 * @param action    what was done, for example {@code LOGIN} or {@code UPDATE}
 * @param resource  what it was done on, as {@code type/id}
 * @param result    whether the action succeeded or failed
 */
public record AuditEntryDto(
        long id,
        Instant timestamp,
        String userId,
        Optional<Role> role,
        String action,
        String resource,
        AuditResult result) {
}
