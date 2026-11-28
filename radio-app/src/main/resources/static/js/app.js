/** Menu page (F-3, CU-20-C): the user of the session, the entries of their roles and the logout. */

export const ROLE_NAMES = Object.freeze({
    PATIENT: 'Patient',
    PHYSICIAN: 'Physician',
    ADMISSIONS: 'Admissions',
    ADMINISTRATOR: 'Administrator',
});

/**
 * Wires the menu page.
 *
 * @param {Document} document
 * @param {object}   api         `js/api.js` (or a fake in the tests): `currentUser`, `logout`
 * @param {Function} entriesFor  `js/menu.js`: the menu entries of some roles
 * @param {Window}   window      for the back button (`pageshow`) and the reload
 */
export async function startMenuPage(document, api, entriesFor, window) {
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
            await api.logout();
        } catch (error) {
            status.textContent = error.message;
            logoutButton.disabled = false;
        }
    });

    try {
        // Without a session, api.js sends the browser to the login page.
        const user = await api.currentUser();
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
}

if (typeof document !== 'undefined') {
    // Dynamic imports, so the unit test can load this file alone with a fake API.
    Promise.all([import('./api.js'), import('./menu.js')])
        .then(([api, menu]) => startMenuPage(document, api, menu.entriesFor, window));
}
