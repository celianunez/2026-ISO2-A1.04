/**
 * Menu of the application (F-3). One entry per use case page, with the roles that may use it.
 *
 * To add your page: one line here, in use case order. `path` is relative to `app.html`. The menu only hides the
 * entry: the server still checks the permission (`@PreAuthorize`).
 *
 * Roles: PATIENT, PHYSICIAN, ADMISSIONS, ADMINISTRATOR (es.uclm.sescam.common.api.Role).
 */
export const MENU = Object.freeze([
    {path: 'cu-18/index.html', label: 'Roles and permissions', roles: ['ADMINISTRATOR']},
]);

/** The entries that a user with these roles may see, in menu order. */
export function entriesFor(roles, menu = MENU) {
    return menu.filter(entry => entry.roles.some(role => roles.includes(role)));
}
