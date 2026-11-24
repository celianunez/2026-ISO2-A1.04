package es.uclm.sescam.common.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated user from the {@link SecurityContextHolder} (D-09).
 *
 * <p>Static helper on purpose: the server checks permissions with {@code @PreAuthorize} and, when
 * it needs the user itself (audit records, "my data" filters), it asks this class instead of
 * passing the {@link Authentication} through every layer.
 *
 * <p>There is no setter: the value comes from the security context of the current thread, which
 * Spring Security fills at login.
 */
public final class CurrentUser {

    private static final String ROLE_PREFIX = "ROLE_";

    private CurrentUser() {
    }

    /**
     * Identifier of the user behind the current request.
     *
     * @return the user id, or empty for an anonymous, unauthenticated or absent principal
     */
    public static Optional<String> findUserId() {
        return currentAuthentication().map(Authentication::getName).filter(id -> !id.isBlank());
    }

    /**
     * Roles of the user behind the current request.
     *
     * <p>Authorities that are not one of the {@link Role} values are ignored instead of failing:
     * an authority of another application must not break our queries.
     *
     * @return an unmodifiable set, empty when there is no authenticated user
     */
    public static Set<Role> findRoles() {
        Optional<Authentication> authentication = currentAuthentication();
        if (authentication.isEmpty()) {
            return Set.of();
        }
        Set<Role> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.get().getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith(ROLE_PREFIX)) {
                name = name.substring(ROLE_PREFIX.length());
            }
            try {
                roles.add(Role.valueOf(name));
            } catch (IllegalArgumentException unknownRole) {
                // Not one of ours: ignored (see the Javadoc).
            }
        }
        return Collections.unmodifiableSet(roles);
    }

    /**
     * @param role the role to look for
     * @return {@code true} when the authenticated user has that role
     */
    public static boolean hasRole(Role role) {
        return findRoles().contains(role);
    }

    /**
     * Same as {@link #findUserId()} but for the places where a user must exist, for example to
     * write an audit record.
     *
     * @return the user id
     * @throws ForbiddenOperationException when nobody is authenticated
     */
    public static String requireUserId() {
        return findUserId().orElseThrow(
                () -> new ForbiddenOperationException("An authenticated user is required"));
    }

    /**
     * The current authentication, ignoring the anonymous one: an anonymous token is
     * "authenticated" by definition but it is not a user.
     */
    private static Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }
}
