package es.uclm.sescam.auth.application;

import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.auth.api.IAuthorizationService;
import es.uclm.sescam.auth.domain.UserAccount;
import es.uclm.sescam.auth.infrastructure.UserAccountRepository;
import es.uclm.sescam.common.api.CurrentUser;
import es.uclm.sescam.common.api.ForbiddenOperationException;
import es.uclm.sescam.common.api.NotFoundException;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Manages local accounts and reads their roles when establishing a session. */
@Service
public class AuthorizationService implements IAuthorizationService {

    // OWN permissions describe a capability; the owning module still checks the resource owner.
    private static final Map<Role, Set<String>> PERMISSIONS = Map.of(
            Role.ADMINISTRATOR, Set.of("CATALOGUE_EDIT", "CONFIGURATION_EDIT", "USER_ROLES_EDIT", "AUDIT_READ"),
            Role.ADMISSIONS, Set.of("PATIENT_MANAGE", "APPOINTMENT_BOOK"),
            Role.PHYSICIAN, Set.of("TEST_PRESCRIBE", "EXAM_RECORD", "REPORT_SIGN"),
            Role.PATIENT, Set.of("OWN_REQUESTS_READ", "OWN_APPOINTMENTS_MANAGE", "OWN_HISTORY_READ"));

    private final UserAccountRepository accounts;
    private final IAuditService audit;
    private final EntityManager entityManager;

    public AuthorizationService(UserAccountRepository accounts, IAuditService audit, EntityManager entityManager) {
        this.accounts = accounts;
        this.audit = audit;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> getRoles(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return accounts.findById(userId)
                .filter(UserAccount::isActive)
                .map(UserAccount::roles)
                .orElseGet(Set::of);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> searchUsers(String query) {
        UserRef administrator = requireAdministrator();
        String term = query == null ? "" : query.strip();
        List<UserSummary> users = accounts
                .findByIdContainingIgnoreCaseOrDisplayNameContainingIgnoreCaseOrderByDisplayNameAscIdAsc(term, term)
                .stream()
                .map(user -> new UserSummary(user.id(), user.displayName(), user.isActive(), user.roles()))
                .toList();
        audit.record(administrator, "READ", "user/search");
        return users;
    }

    @Override
    @Transactional
    public void assignRole(String userId, Role role) {
        UserRef administrator = requireAdministrator();
        UserAccount account = findForRoleChange(userId);
        if (account.assignRole(role)) {
            recordRoleChange(administrator, "ASSIGN_ROLE", userId, role);
        }
    }

    @Override
    @Transactional
    public void removeRole(String userId, Role role) {
        UserRef administrator = requireAdministrator();
        UserAccount account = findForRoleChange(userId);
        if (account.removeRole(role)) {
            recordRoleChange(administrator, "REMOVE_ROLE", userId, role);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(String userId, String permission) {
        if (userId == null || userId.isBlank() || permission == null || permission.isBlank()) {
            return false;
        }
        // A role edit changes the next session, never the current session's authorities.
        Set<Role> roles = CurrentUser.findUserId().filter(userId::equals).isPresent()
                ? CurrentUser.findRoles()
                : getRoles(userId);
        return roles.stream().anyMatch(role -> PERMISSIONS.getOrDefault(role, Set.of()).contains(permission));
    }

    private UserAccount findForRoleChange(String userId) {
        return accounts.findForRoleChange(userId)
                .orElseThrow(() -> new NotFoundException("No user with id " + userId));
    }

    private void recordRoleChange(UserRef administrator, String action, String userId, Role role) {
        // Flush first so database constraint failures do not create a success audit entry.
        entityManager.flush();
        audit.record(administrator, action, "user/" + userId + "/roles/" + role.name());
    }

    private UserRef requireAdministrator() {
        if (!CurrentUser.hasRole(Role.ADMINISTRATOR)) {
            throw new ForbiddenOperationException("Administrator role required");
        }
        String administratorId = CurrentUser.requireUserId();
        return new UserRef(administratorId, administratorId, Role.ADMINISTRATOR);
    }

    public record UserSummary(String id, String displayName, boolean active, Set<Role> roles) {
    }
}
