package es.uclm.sescam.auth.api;

import es.uclm.sescam.common.api.Role;
import java.util.Set;

/**
 * A logged-in user. It is also the JSON body of {@code POST /api/auth/login} and {@code GET /api/auth/me}:
 * {@code { "id": "...", "displayName": "...", "roles": [ ... ] }}.
 *
 * @param id          corporate user id
 * @param displayName name to show in the application
 * @param roles       roles of the user in this application; never empty for a logged-in user
 */
public record AuthenticatedUserDto(String id, String displayName, Set<Role> roles) {

    /**
     * Creates the user and makes the set of roles unmodifiable.
     *
     * @param id          corporate user id
     * @param displayName name to show in the application
     * @param roles       roles of the user in this application
     */
    public AuthenticatedUserDto {
        roles = Set.copyOf(roles);
    }
}
