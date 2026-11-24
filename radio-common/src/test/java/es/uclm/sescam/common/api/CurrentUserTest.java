package es.uclm.sescam.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class CurrentUserTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEmptyWhenThereIsNoAuthentication() {
        assertTrue(CurrentUser.findUserId().isEmpty());
        assertTrue(CurrentUser.findRoles().isEmpty());
        assertFalse(CurrentUser.hasRole(Role.PHYSICIAN));
        assertThrows(ForbiddenOperationException.class, CurrentUser::requireUserId);
    }

    @Test
    void readsTheUserIdAndTheRolesOfTheAuthenticatedUser() {
        authenticate("u-7", "ROLE_PHYSICIAN");

        assertEquals(Optional.of("u-7"), CurrentUser.findUserId());
        assertEquals(Set.of(Role.PHYSICIAN), CurrentUser.findRoles());
        assertTrue(CurrentUser.hasRole(Role.PHYSICIAN));
        assertFalse(CurrentUser.hasRole(Role.ADMINISTRATOR));
        assertEquals("u-7", CurrentUser.requireUserId());
    }

    @Test
    void readsEveryRoleOfTheUser() {
        authenticate("u-8", "ROLE_ADMISSIONS", "ROLE_ADMINISTRATOR");

        assertEquals(Set.of(Role.ADMISSIONS, Role.ADMINISTRATOR), CurrentUser.findRoles());
    }

    @Test
    void ignoresAuthoritiesThatAreNotOurRoles() {
        authenticate("u-9", "ROLE_GHOST", "ROLE_PHYSICIAN");

        assertEquals(Set.of(Role.PHYSICIAN), CurrentUser.findRoles());
    }

    @Test
    void returnsEmptyForAnUnauthenticatedPrincipal() {
        TestingAuthenticationToken unauthenticated = new TestingAuthenticationToken("u-10", "secret");
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        assertTrue(CurrentUser.findUserId().isEmpty());
        assertTrue(CurrentUser.findRoles().isEmpty());
        assertThrows(ForbiddenOperationException.class, CurrentUser::requireUserId);
    }

    @Test
    void returnsEmptyForTheAnonymousAuthentication() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertTrue(CurrentUser.findUserId().isEmpty());
        assertTrue(CurrentUser.findRoles().isEmpty());
        assertThrows(ForbiddenOperationException.class, CurrentUser::requireUserId);
    }

    @Test
    void takesTheUsernameFromAUserDetailsPrincipal() {
        Authentication authentication = new TestingAuthenticationToken(
                User.withUsername("u-11").password("x").roles("PATIENT").build(), "n/a",
                AuthorityUtils.createAuthorityList(Role.PATIENT.authority()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(Optional.of("u-11"), CurrentUser.findUserId());
        assertEquals(Set.of(Role.PATIENT), CurrentUser.findRoles());
    }

    /** Authenticates a user with the given authorities in the security context of the test. */
    private void authenticate(String userId, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                userId, "n/a", AuthorityUtils.createAuthorityList(authorities)));
    }
}
