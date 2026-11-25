package es.uclm.sescam.audit.infrastructure;

import es.uclm.sescam.audit.domain.AuditEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Storage of the audit log. It extends the marker {@link Repository} (not {@code CrudRepository} or
 * {@code JpaRepository}) so that it exposes <strong>only</strong> {@code save} and queries: no update and no
 * delete method exists (D-10).
 */
public interface AuditRepository extends Repository<AuditEntry, Long> {

    /**
     * Stores a new entry.
     *
     * @param entry the entry to store
     * @return the stored entry, with its identifier
     */
    AuditEntry save(AuditEntry entry);

    /**
     * Finds the entries that match every filter, newest first. A {@code null} user is ignored.
     *
     * @param userId          exact user identifier, or {@code null}
     * @param from            lower bound of the timestamp (inclusive)
     * @param to              upper bound of the timestamp (inclusive)
     * @param resourcePattern LIKE pattern for the resource, already escaped with {@code !} and ending in
     *                        {@code %}; {@code %} matches everything
     * @return matching entries, newest first
     */
    @Query("""
            select e from AuditEntry e
            where (:userId is null or e.userId = :userId)
              and e.timestamp >= :from
              and e.timestamp <= :to
              and e.resource like :resourcePattern escape '!'
            order by e.timestamp desc, e.id desc
            """)
    List<AuditEntry> search(@Param("userId") String userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("resourcePattern") String resourcePattern);
}
