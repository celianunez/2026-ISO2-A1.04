package es.uclm.sescam.audit.domain;

import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.common.api.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One entry of the audit log. Immutable: there are no setters, every column is {@code updatable = false}
 * and Hibernate treats the entity as read-only once stored ({@link Immutable}).
 */
@Entity
@Immutable
@Table(name = "audit_entry")
public class AuditEntry {

    /** Longest user id that is stored. */
    public static final int MAX_USER_ID_LENGTH = 64;

    /** Longest action text that is stored. */
    public static final int MAX_ACTION_LENGTH = 32;

    /** Longest resource text that is stored. */
    public static final int MAX_RESOURCE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "user_id", nullable = false, updatable = false, length = MAX_USER_ID_LENGTH)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", updatable = false, length = 32)
    private Role role;

    @Column(name = "action", nullable = false, updatable = false, length = MAX_ACTION_LENGTH)
    private String action;

    @Column(name = "resource", nullable = false, updatable = false, length = MAX_RESOURCE_LENGTH)
    private String resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, updatable = false, length = 16)
    private AuditResult result;

    /** For JPA only. */
    protected AuditEntry() {
    }

    private AuditEntry(Instant timestamp, String userId, Role role, String action, String resource,
            AuditResult result) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.role = role;
        this.action = action;
        this.resource = resource;
        this.result = result;
    }

    /**
     * Creates a new, not yet stored, entry after checking the business rules. Texts are stored without
     * leading or trailing spaces.
     *
     * @param timestamp when the action happened
     * @param userId    who did it; not blank, at most {@value #MAX_USER_ID_LENGTH} characters
     * @param role      role used, or {@code null} if nobody was authenticated
     * @param action    what was done, for example {@code READ}; not blank, at most {@value #MAX_ACTION_LENGTH}
     *                  characters
     * @param resource  what it was done on, {@code type/id}; not blank, at most {@value #MAX_RESOURCE_LENGTH}
     *                  characters
     * @param result    outcome; not {@code null}
     * @return the new entry
     * @throws IllegalArgumentException if a rule is broken
     */
    public static AuditEntry create(Instant timestamp, String userId, Role role, String action, String resource,
            AuditResult result) {
        requireArgument(timestamp != null, "timestamp is required");
        requireArgument(result != null, "result is required");
        String cleanUserId = requireText(userId, "userId", MAX_USER_ID_LENGTH);
        String cleanAction = requireText(action, "action", MAX_ACTION_LENGTH);
        String cleanResource = requireText(resource, "resource", MAX_RESOURCE_LENGTH);
        return new AuditEntry(timestamp, cleanUserId, role, cleanAction, cleanResource, result);
    }

    private static String requireText(String text, String name, int maxLength) {
        requireArgument(text != null && !text.isBlank(), name + " is required");
        String clean = text.strip();
        requireArgument(clean.length() <= maxLength, name + " is longer than " + maxLength + " characters");
        return clean;
    }

    private static void requireArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Role the user acted with.
     *
     * @return the role, or empty if nobody was authenticated
     */
    public Optional<Role> getRole() {
        return Optional.ofNullable(role);
    }

    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }

    public AuditResult getResult() {
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditEntry that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
