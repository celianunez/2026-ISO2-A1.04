package es.uclm.sescam.auth.domain;

import es.uclm.sescam.common.api.Role;
import java.util.Objects;
import java.util.Set;

/** A local account identified by its corporate user ID. */
public record UserAccount(String id, String displayName, boolean active, Set<Role> roles) {

    public UserAccount {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        roles = Set.copyOf(roles);
    }
}
