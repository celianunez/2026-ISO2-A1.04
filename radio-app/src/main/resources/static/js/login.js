/** Login page (F-3, CU-20-C): simulated corporate SSO. */

/** The simulated SSO issues the token `token-<userId>` (SimulatedSsoGateway). */
export const SSO_TOKEN_PREFIX = 'token-';

/**
 * Wires the login page.
 *
 * @param {Document} document
 * @param {object}   api       `js/api.js` (or a fake in the tests): `login`, `currentUser`, `MENU_PAGE`
 * @param {Location} location  where to go after the login
 */
export async function startLoginPage(document, api, location) {
    const form = document.getElementById('login-form');
    const user = document.getElementById('login-user');
    const submit = document.getElementById('login-submit');
    const status = document.getElementById('login-status');

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (submit.disabled) return;
        submit.disabled = true;
        status.textContent = '';
        try {
            await api.login(SSO_TOKEN_PREFIX + user.value);
            location.assign(api.MENU_PAGE);
        } catch (error) {
            status.textContent = error.message || 'The login could not be completed.';
            submit.disabled = false;
        }
    });

    if (new URLSearchParams(location.search).has('ended')) {
        status.textContent = 'Your session has ended. Log in again.';
    }

    // Already logged in (for example, the login page opened in a new tab): straight to the menu.
    try {
        await api.currentUser({redirectOn401: false});
        location.replace(api.MENU_PAGE);
    } catch {
        // No session: stay on the login page.
    }
}

if (typeof document !== 'undefined') {
    // Dynamic import, so the unit test can load this file alone with a fake API.
    import('./api.js').then(api => startLoginPage(document, api, window.location));
}
