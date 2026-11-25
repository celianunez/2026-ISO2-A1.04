package es.uclm.sescam.auth.api;

import es.uclm.sescam.common.api.Role;
import java.util.Set;

/**
 * Roles and permissions of the users (CU-18). Implemented by the users-and-roles part of {@code radio-auth}.
 *
 * <p>Only administrators may change roles; the check is done with Spring Security (D-09). Every change is audited.
 * A role change takes effect from the user's next session.
 */
public interface IAuthorizationService {

    /**
     * Gives a role to a user. Nothing changes if the user already has it.
     *
     * @param userId corporate user id
     * @param role   role to give; must not be null
     * @throws es.uclm.sescam.common.api.NotFoundException if the user does not exist
     */
    void assignRole(String userId, Role role);

    /**
     * Withdraws a role from a user. Nothing changes if the user does not have it.
     *
     * @param userId corporate user id
     * @param role   role to withdraw; must not be null
     * @throws es.uclm.sescam.common.api.NotFoundException if the user does not exist
     */
    void removeRole(String userId, Role role);

    /**
     * Returns the roles of a user. The login uses it to deny access to an identity without a role (CU-20).
     *
     * @param userId corporate user id
     * @return the roles of the user; empty if the user does not exist, is inactive or has no role
     */
    Set<Role> getRoles(String userId);

    /**
     * Tells whether one of the roles of a user grants a permission of the roles and permissions matrix (CU-18).
     *
     * @param userId     corporate user id
     * @param permission permission code of the matrix, for example {@code CATALOGUE_EDIT}
     * @return {@code true} if the user has the permission; {@code false} for an unknown user or permission
     */
    boolean hasPermission(String userId, String permission);
}
