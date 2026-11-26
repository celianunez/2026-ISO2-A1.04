package es.uclm.sescam.auth.application;

import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.auth.api.AuthenticatedUserDto;
import es.uclm.sescam.auth.api.AuthenticationFailedException;
import es.uclm.sescam.auth.api.AuthenticationFailedException.Reason;
import es.uclm.sescam.auth.api.IAuthenticationService;
import es.uclm.sescam.auth.api.IAuthorizationService;
import es.uclm.sescam.common.api.CurrentUser;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import es.uclm.sescam.ssoadapter.api.CorporateIdentityDto;
import es.uclm.sescam.ssoadapter.api.ISsoGateway;
import es.uclm.sescam.ssoadapter.api.InvalidSsoTokenException;
import es.uclm.sescam.ssoadapter.api.SsoUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/**
 * Login with the corporate SSO (CU-20-S): identity from {@link ISsoGateway}, roles from
 * {@link IAuthorizationService} (CU-18), every attempt audited (CU-19).
 */
@Service
public class AuthenticationService implements IAuthenticationService {

    static final String LOGIN = "LOGIN";
    static final String LOGOUT = "LOGOUT";
    static final String LOGIN_RESOURCE = "auth/login";
    static final String LOGOUT_RESOURCE = "auth/logout";
    static final String SSO_RESOURCE = "auth/sso";

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final ISsoGateway sso;
    private final IAuthorizationService authorization;
    private final IAuditService audit;

    public AuthenticationService(ISsoGateway sso, IAuthorizationService authorization, IAuditService audit) {
        this.sso = sso;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public AuthenticatedUserDto loginWithSso(String token) {
        CorporateIdentityDto identity;
        try {
            identity = sso.authenticate(token);
        } catch (InvalidSsoTokenException rejected) {
            audit.record(null, LOGIN, LOGIN_RESOURCE, AuditResult.FAILURE);
            throw new AuthenticationFailedException(Reason.INVALID_TOKEN, "The corporate login was not accepted");
        } catch (SsoUnavailableException unavailable) {
            // Integration failure (D-08): recorded, and the rest of the system keeps working.
            log.warn("Login failed because the corporate SSO is not available: {}", unavailable.getMessage());
            audit.record(null, LOGIN, SSO_RESOURCE, AuditResult.FAILURE);
            throw unavailable;
        }

        Set<Role> roles = authorization.getRoles(identity.userId());
        if (roles.isEmpty()) {
            audit.record(new UserRef(identity.userId(), identity.displayName(), null), LOGIN, LOGIN_RESOURCE,
                    AuditResult.FAILURE);
            throw new AuthenticationFailedException(Reason.NO_ROLE,
                    "Your corporate user has no access to this application");
        }

        AuthenticatedUserDto user = new AuthenticatedUserDto(identity.userId(), identity.displayName(), roles);
        audit.record(toUserRef(user), LOGIN, LOGIN_RESOURCE, AuditResult.SUCCESS);
        return user;
    }

    @Override
    public Optional<AuthenticatedUserDto> currentUser() {
        return CurrentUser.findUserId().map(id -> {
            Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
            String displayName = details instanceof String name ? name : id;
            return new AuthenticatedUserDto(id, displayName, CurrentUser.findRoles());
        });
    }

    @Override
    public void logout() {
        currentUser().ifPresent(user -> audit.record(toUserRef(user), LOGOUT, LOGOUT_RESOURCE, AuditResult.SUCCESS));
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds the Spring Security authentication that the web layer stores in the session for a logged-in user. The
     * principal is the user id (so {@link CurrentUser} works in every module), the authorities are the roles and the
     * details hold the display name. Only strings are stored, so the session stays serializable.
     *
     * @param user the user returned by {@link #loginWithSso(String)}
     * @return an authenticated token for that user
     */
    public static Authentication authenticationFor(AuthenticatedUserDto user) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                user.id(), null,
                user.roles().stream().map(role -> new SimpleGrantedAuthority(role.authority())).toList());
        authentication.setDetails(user.displayName());
        return authentication;
    }

    /** The audit entry has one role: the first role of the user in the order of {@link Role}. */
    private static UserRef toUserRef(AuthenticatedUserDto user) {
        Role role = user.roles().stream().min(Comparator.naturalOrder()).orElse(null);
        return new UserRef(user.id(), user.displayName(), role);
    }
}
