package es.uclm.sescam.auth.application;

import es.uclm.sescam.audit.api.AuditResult;
import es.uclm.sescam.audit.api.IAuditService;
import es.uclm.sescam.auth.api.AuthenticatedUserDto;
import es.uclm.sescam.auth.api.AuthenticationFailedException;
import es.uclm.sescam.auth.api.AuthenticationFailedException.Reason;
import es.uclm.sescam.auth.api.IAuthorizationService;
import es.uclm.sescam.common.api.CurrentUser;
import es.uclm.sescam.common.api.Role;
import es.uclm.sescam.common.api.UserRef;
import es.uclm.sescam.ssoadapter.api.CorporateIdentityDto;
import es.uclm.sescam.ssoadapter.api.ISsoGateway;
import es.uclm.sescam.ssoadapter.api.InvalidSsoTokenException;
import es.uclm.sescam.ssoadapter.api.SsoUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the login logic (CU-20-S, test cases 4 to 8 and 15). The SSO, the roles and the audit are mocks.
 */
class AuthenticationServiceTest {

    private final ISsoGateway sso = mock(ISsoGateway.class);
    private final IAuthorizationService authorization = mock(IAuthorizationService.class);
    private final IAuditService audit = mock(IAuditService.class);
    private final AuthenticationService service = new AuthenticationService(sso, authorization, audit);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsInAUserWithOneRoleAndAuditsTheSuccess() {
        when(sso.authenticate("token-physician01"))
                .thenReturn(new CorporateIdentityDto("physician01", "Demo Physician One"));
        when(authorization.getRoles("physician01")).thenReturn(Set.of(Role.PHYSICIAN));

        AuthenticatedUserDto user = service.loginWithSso("token-physician01");

        assertThat(user).isEqualTo(
                new AuthenticatedUserDto("physician01", "Demo Physician One", Set.of(Role.PHYSICIAN)));
        verify(audit).record(new UserRef("physician01", "Demo Physician One", Role.PHYSICIAN), "LOGIN", "auth/login",
                AuditResult.SUCCESS);
    }

    @Test
    void logsInAUserWithTwoRolesAndKeepsBoth() {
        when(sso.authenticate("token-physicianadmin01"))
                .thenReturn(new CorporateIdentityDto("physicianadmin01", "Demo Physician Administrator"));
        when(authorization.getRoles("physicianadmin01")).thenReturn(Set.of(Role.PHYSICIAN, Role.ADMINISTRATOR));

        AuthenticatedUserDto user = service.loginWithSso("token-physicianadmin01");

        assertThat(user.roles()).containsExactlyInAnyOrder(Role.PHYSICIAN, Role.ADMINISTRATOR);
        // The audit entry keeps one role: the first one in the order of Role.
        verify(audit).record(new UserRef("physicianadmin01", "Demo Physician Administrator", Role.PHYSICIAN),
                "LOGIN", "auth/login", AuditResult.SUCCESS);
    }

    @Test
    void rejectsAnInvalidTokenAndAuditsTheFailureWithoutUser() {
        when(sso.authenticate("token-unknown")).thenThrow(new InvalidSsoTokenException("not recognised"));

        assertThatThrownBy(() -> service.loginWithSso("token-unknown"))
                .isInstanceOfSatisfying(AuthenticationFailedException.class,
                        rejected -> assertThat(rejected.reason()).isEqualTo(Reason.INVALID_TOKEN));

        verify(audit).record(isNull(), eq("LOGIN"), eq("auth/login"), eq(AuditResult.FAILURE));
        verifyNoInteractions(authorization);
    }

    @Test
    void deniesAnIdentityWithoutRoleAndAuditsTheFailure() {
        when(sso.authenticate("token-norole01")).thenReturn(new CorporateIdentityDto("norole01", "Demo No Role"));
        when(authorization.getRoles("norole01")).thenReturn(Set.of());

        assertThatThrownBy(() -> service.loginWithSso("token-norole01"))
                .isInstanceOfSatisfying(AuthenticationFailedException.class,
                        rejected -> assertThat(rejected.reason()).isEqualTo(Reason.NO_ROLE));

        verify(audit).record(new UserRef("norole01", "Demo No Role", null), "LOGIN", "auth/login",
                AuditResult.FAILURE);
    }

    @Test
    void passesOnAnUnavailableSsoAndAuditsTheIntegrationFailure() {
        when(sso.authenticate(anyString())).thenThrow(new SsoUnavailableException("down"));

        assertThatThrownBy(() -> service.loginWithSso("token-patient01")).isInstanceOf(SsoUnavailableException.class);

        verify(audit).record(isNull(), eq("LOGIN"), eq("auth/sso"), eq(AuditResult.FAILURE));
        verifyNoInteractions(authorization);
    }

    @Test
    void hasNoCurrentUserWithoutSession() {
        assertThat(service.currentUser()).isEmpty();
    }

    @Test
    void readsTheCurrentUserFromTheSecurityContext() {
        AuthenticatedUserDto user =
                new AuthenticatedUserDto("admin01", "Demo Administrator", Set.of(Role.ADMINISTRATOR));
        Authentication authentication = AuthenticationService.authenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(service.currentUser()).contains(user);
        // Other modules see the same user through radio-common.
        assertThat(CurrentUser.findUserId()).contains("admin01");
        assertThat(CurrentUser.findRoles()).containsExactly(Role.ADMINISTRATOR);
    }

    @Test
    void logoutAuditsAndClearsTheSecurityContext() {
        AuthenticatedUserDto user = new AuthenticatedUserDto("patient01", "Demo Patient One", Set.of(Role.PATIENT));
        SecurityContextHolder.getContext().setAuthentication(AuthenticationService.authenticationFor(user));

        service.logout();

        verify(audit).record(new UserRef("patient01", "Demo Patient One", Role.PATIENT), "LOGOUT", "auth/logout",
                AuditResult.SUCCESS);
        assertThat(service.currentUser()).isEmpty();
    }

    @Test
    void logoutWithoutSessionDoesNothing() {
        service.logout();

        verify(audit, never()).record(any(), anyString(), anyString(), any());
    }
}
