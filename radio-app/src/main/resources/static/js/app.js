/** Menu page (F-3): the user of the session, the entries of their roles and the logout. */
import {currentUser, logout} from './api.js';
import {entriesFor} from './menu.js';

const ROLE_NAMES = Object.freeze({
    PATIENT: 'Patient',
    PHYSICIAN: 'Physician',
    ADMISSIONS: 'Admissions',
    ADMINISTRATOR: 'Administrator',
});

const status = document.getElementById('app-status');
const userLabel = document.getElementById('app-user');
const logoutButton = document.getElementById('app-logout');
const menu = document.getElementById('app-menu');
const entries = document.getElementById('app-entries');

function renderEntry(entry) {
    const item = document.createElement('li');
    const link = document.createElement('a');
    link.href = entry.path;
    link.textContent = entry.label;
    item.append(link);
    return item;
}

// Back button after logout: a page restored from the browser cache asks the server again.
window.addEventListener('pageshow', event => {
    if (event.persisted) window.location.reload();
});

logoutButton.addEventListener('click', async () => {
    logoutButton.disabled = true;
    try {
        await logout();
    } catch (error) {
        status.textContent = error.message;
        logoutButton.disabled = false;
    }
});

try {
    // Without a session, api.js sends the browser to the login page.
    const user = await currentUser();
    const roleNames = user.roles.map(role => ROLE_NAMES[role] || role).join(', ');
    userLabel.textContent = user.displayName + ' · ' + roleNames;
    userLabel.hidden = false;
    logoutButton.hidden = false;

    const visible = entriesFor(user.roles);
    entries.replaceChildren(...visible.map(renderEntry));
    menu.hidden = false;
    status.textContent = visible.length ? '' : 'There are no pages for your role yet.';
} catch (error) {
    if (error.status !== 401) status.textContent = error.message;
}
