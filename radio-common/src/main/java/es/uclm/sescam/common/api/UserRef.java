package es.uclm.sescam.common.api;

/**
 * The minimal reference to an authenticated user that other modules may keep in memory.
 *
 * <p>It is a value object: immutable, no setters, never {@code null}. Modules pass it around
 * instead of leaking Spring Security classes or another module's user tables.
 *
 * @param id the identifier of the user inside the system
 * @param displayName the name shown in the UI and written in the audit records
 * @param role the role the user is acting with in this session
 */
public record UserRef(String id, String displayName, Role role) {
}
