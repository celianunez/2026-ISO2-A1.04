package es.uclm.sescam.auth.api;

import java.util.Optional;

/**
 * Login and session of the users (CU-20).
 *
 * <p>The identity comes from the corporate SSO ({@code ISsoGateway}) and the roles from
 * {@link IAuthorizationService} (CU-18). Other modules do not use this interface to know who the user is: they use
 * {@code CurrentUser} from {@code radio-common} (D-09).
 */
public interface IAuthenticationService {

    /**
     * Logs a user in with a token of the corporate SSO. Every attempt, successful or not, is audited. The web layer
     * stores the returned user in the HTTP session (Spring Security).
     *
     * @param token token issued by the corporate SSO; must not be null or blank
     * @return the authenticated user, with at least one role
     * @throws AuthenticationFailedException with reason {@code INVALID_TOKEN} if the token is unknown or has expired,
     *                                       or {@code NO_ROLE} if the identity has no role in this application
     * @throws es.uclm.sescam.ssoadapter.api.SsoUnavailableException if the corporate SSO cannot be reached
     */
    AuthenticatedUserDto loginWithSso(String token);

    /**
     * Returns the user of the current session.
     *
     * @return the logged-in user, or empty if nobody is logged in
     */
    Optional<AuthenticatedUserDto> currentUser();

    /**
     * Ends the session of the current user and audits the logout. Does nothing if nobody is logged in.
     */
    void logout();
}
