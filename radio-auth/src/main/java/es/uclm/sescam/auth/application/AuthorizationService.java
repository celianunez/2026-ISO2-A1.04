package es.uclm.sescam.auth.application;

import es.uclm.sescam.auth.api.IAuthorizationService;
import es.uclm.sescam.auth.domain.UserAccount;
import es.uclm.sescam.auth.infrastructure.UserAccountRepository;
import es.uclm.sescam.common.api.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Reads the current account roles when establishing a new session. */
@Service
public class AuthorizationService implements IAuthorizationService {

    private final UserAccountRepository accounts;

    public AuthorizationService(UserAccountRepository accounts) {
        this.accounts = accounts;
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

    @Override
    public void assignRole(String userId, Role role) {
        throw new UnsupportedOperationException("Role changes are not available in this increment");
    }

    @Override
    public void removeRole(String userId, Role role) {
        throw new UnsupportedOperationException("Role changes are not available in this increment");
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        // Deny access until the CU-18 permission matrix is implemented.
        return false;
    }
}
