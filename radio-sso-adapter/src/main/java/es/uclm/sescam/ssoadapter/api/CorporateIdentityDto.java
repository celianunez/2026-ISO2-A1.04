package es.uclm.sescam.ssoadapter.api;

/**
 * Identity of a user as known by the corporate SSO. It carries no roles: the roles of the user in this application
 * are kept by {@code radio-auth} (CU-18) under the same {@code userId}.
 *
 * @param userId      corporate user id; {@code radio-auth} uses the same id for the user's roles
 * @param displayName name to show in the application (fictitious data only)
 */
public record CorporateIdentityDto(String userId, String displayName) {
}
