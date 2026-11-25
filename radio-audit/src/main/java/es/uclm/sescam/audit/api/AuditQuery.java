package es.uclm.sescam.audit.api;

import java.time.Instant;

/**
 * Filters for {@link IAuditService#query(AuditQuery)}. A {@code null} field means "no filter on that field".
 *
 * @param userId   only entries of this user id
 * @param from     only entries at or after this instant (inclusive)
 * @param to       only entries at or before this instant (inclusive)
 * @param resource only entries whose resource starts with this text, for example {@code patient/} for every patient
 */
public record AuditQuery(String userId, Instant from, Instant to, String resource) {

    /**
     * Returns a query without filters.
     *
     * @return a query that matches every entry
     */
    public static AuditQuery all() {
        return new AuditQuery(null, null, null, null);
    }
}
