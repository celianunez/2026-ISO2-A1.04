/** Login page (F-3, CU-20-C): simulated corporate SSO. */
import {MENU_PAGE, currentUser, login} from './api.js';

const SSO_TOKEN_PREFIX = 'token-';

const form = document.getElementById('login-form');
const user = document.getElementById('login-user');
const submit = document.getElementById('login-submit');
const status = document.getElementById('login-status');

function showError(error) {
    status.textContent = error.message || 'The login could not be completed.';
}

form.addEventListener('submit', async event => {
    event.preventDefault();
    submit.disabled = true;
    status.textContent = '';
    try {
        await login(SSO_TOKEN_PREFIX + user.value);
        window.location.assign(MENU_PAGE);
    } catch (error) {
        showError(error);
        submit.disabled = false;
    }
});

if (new URLSearchParams(window.location.search).has('ended')) {
    status.textContent = 'Your session has ended. Log in again.';
}

// Already logged in (for example, the login page opened in a new tab): straight to the menu.
try {
    await currentUser({redirectOn401: false});
    window.location.replace(MENU_PAGE);
} catch {
    // No session: stay on the login page.
}
