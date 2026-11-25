package es.uclm.sescam.ssoadapter.api;

import java.util.Optional;

/**
 * Gateway to the corporate single sign-on of the hospital (CU-20, D-08).
 *
 * <p>The real SSO would use OpenID Connect; in this project it is simulated (D-27). The simulator has fictitious
 * users, one or more per role, whose tokens are simple opaque strings, and a failure mode in which every call throws
 * {@link SsoUnavailableException}.
 */
public interface ISsoGateway {

    /**
     * Validates an SSO token at login and returns the identity it belongs to.
     *
     * @param token token issued by the corporate SSO; must not be null or blank
     * @return the corporate identity of the owner of the token
     * @throws InvalidSsoTokenException if the SSO does not recognise the token or the token has expired
     * @throws SsoUnavailableException  if the SSO cannot be reached
     */
    CorporateIdentityDto authenticate(String token);

    /**
     * Returns the identity of a token that is still valid, without failing if it is not. Lets a user who is already
     * authenticated in the SSO enter the application without identifying again.
     *
     * @param token token issued by the corporate SSO; must not be null or blank
     * @return the corporate identity, or empty if the token is unknown or has expired
     * @throws SsoUnavailableException if the SSO cannot be reached
     */
    Optional<CorporateIdentityDto> getCorporateIdentity(String token);
}
