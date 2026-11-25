package es.uclm.sescam.auth.domain;

import es.uclm.sescam.common.api.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** A local account identified by its corporate user ID. */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ElementCollection
    @CollectionTable(name = "user_account_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private Set<Role> roles = new HashSet<>();

    protected UserAccount() {
        // Required by JPA.
    }

    public UserAccount(String id, String displayName, boolean active, Set<Role> roles) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.active = active;
        this.roles.addAll(Set.copyOf(roles));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean assignRole(Role role) {
        return roles.add(Objects.requireNonNull(role, "role"));
    }

    public boolean removeRole(Role role) {
        return roles.remove(Objects.requireNonNull(role, "role"));
    }

    public boolean isActive() {
        return active;
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }
}
