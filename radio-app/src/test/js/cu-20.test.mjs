// CU-20-C Log in with the corporate SSO (client): the login page and the menu page against a fake server.
// Run: node --test radio-app/src/test/js/
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

async function load(path) {
    const source = await readFile(new URL('../../main/resources/static/' + path, import.meta.url), 'utf8');
    return import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
}

const { startLoginPage, SSO_TOKEN_PREFIX } = await load('js/login.js');
const { startMenuPage } = await load('js/app.js');
const { entriesFor } = await load('js/menu.js');

class Element {
    children = [];
    events = {};
    textContent = '';
    value = '';
    hidden = false;
    disabled = false;
    href = '';

    constructor(tag) { this.tag = tag; }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this.children = nodes; }
    addEventListener(name, callback) { this.events[name] = callback; }
    links() { return this.children.flatMap(item => item.children); }
}

function fakeDocument(ids, hiddenIds = []) {
    const elements = Object.fromEntries(ids.map(id => [id, new Element(id)]));
    for (const id of hiddenIds) elements[id].hidden = true;
    return {
        elements,
        document: { getElementById: id => elements[id], createElement: tag => new Element(tag) },
    };
}

function fakeLocation(search = '') {
    const location = { search, assigned: [], replaced: [], reloads: 0 };
    location.assign = url => location.assigned.push(url);
    location.replace = url => location.replaced.push(url);
    location.reload = () => { location.reloads += 1; };
    return location;
}

function apiError(status, error, message) {
    return Object.assign(new Error(message), { name: 'ApiError', status, error });
}

const NO_SESSION = apiError(401, 'unauthenticated', 'Login required');
const submitEvent = () => ({ preventDefault() {} });

// ---------------------------------------------------------------- login page

function loginPage({ search = '', login, session = NO_SESSION } = {}) {
    const page = fakeDocument(['login-form', 'login-user', 'login-submit', 'login-status']);
    const location = fakeLocation(search);
    const sent = [];
    const api = {
        MENU_PAGE: '/app.html',
        login: async token => {
            sent.push(token);
            return login(token);
        },
        currentUser: async options => {
            assert.deepEqual(options, { redirectOn401: false }, 'the login page must not redirect on 401');
            if (session instanceof Error) throw session;
            return session;
        },
    };
    return { ...page, location, sent, api, start: () => startLoginPage(page.document, api, location) };
}

for (const id of ['patient01', 'physician01', 'admissions01', 'admin01', 'physicianadmin01']) {
    test(`${id} logs in with the SSO token and goes to the menu`, async () => {
        const page = loginPage({ login: async () => ({ id, displayName: 'Demo', roles: ['PATIENT'] }) });
        await page.start();
        page.elements['login-user'].value = id;

        await page.elements['login-form'].events.submit(submitEvent());

        assert.deepEqual(page.sent, [SSO_TOKEN_PREFIX + id]);
        assert.deepEqual(page.location.assigned, ['/app.html']);
        assert.equal(page.elements['login-status'].textContent, '');
    });
}

const rejections = [
    ['an unknown or expired token', apiError(401, 'invalid_token', 'The corporate login was not accepted')],
    ['an identity without a role', apiError(403, 'no_role', 'Your corporate user has no access to this application')],
    ['the SSO out of service', apiError(503, 'sso_unavailable', 'Corporate login is not available, try again later')],
    ['a server that cannot be reached', apiError(0, 'unreachable',
        'The server cannot be reached. Check the connection and try again.')],
];

for (const [name, error] of rejections) {
    test(`${name}: the server message is shown and the user stays on the login page`, async () => {
        const page = loginPage({ login: async () => { throw error; } });
        await page.start();
        page.elements['login-user'].value = 'norole01';

        await page.elements['login-form'].events.submit(submitEvent());

        assert.equal(page.elements['login-status'].textContent, error.message);
        assert.deepEqual(page.location.assigned, []);
        assert.equal(page.elements['login-submit'].disabled, false, 'the user can try again');
    });
}

test('an error without a message gives a generic text', async () => {
    const page = loginPage({ login: async () => { throw new Error(''); } });
    await page.start();
    await page.elements['login-form'].events.submit(submitEvent());
    assert.equal(page.elements['login-status'].textContent, 'The login could not be completed.');
});

test('the button is disabled while the login is running, so a double click sends one login', async () => {
    let finish;
    const page = loginPage({ login: () => new Promise(resolve => { finish = resolve; }) });
    await page.start();
    page.elements['login-user'].value = 'admin01';

    const first = page.elements['login-form'].events.submit(submitEvent());
    assert.equal(page.elements['login-submit'].disabled, true);
    await page.elements['login-form'].events.submit(submitEvent());
    finish({ id: 'admin01', roles: ['ADMINISTRATOR'] });
    await first;

    assert.deepEqual(page.sent, ['token-admin01']);
    assert.deepEqual(page.location.assigned, ['/app.html']);
});

test('after an expired session the login page says so', async () => {
    const page = loginPage({ search: '?ended=1' });
    await page.start();
    assert.equal(page.elements['login-status'].textContent, 'Your session has ended. Log in again.');
    assert.deepEqual(page.location.replaced, []);
});

test('without the notice the status is empty', async () => {
    const page = loginPage();
    await page.start();
    assert.equal(page.elements['login-status'].textContent, '');
});

test('a user who already has a session goes straight to the menu without logging in again', async () => {
    const page = loginPage({ session: { id: 'admin01', displayName: 'Demo Administrator', roles: ['ADMINISTRATOR'] } });
    await page.start();
    assert.deepEqual(page.location.replaced, ['/app.html']);
    assert.deepEqual(page.sent, []);
});

// ----------------------------------------------------------------- menu page

const MENU_IDS = ['app-status', 'app-user', 'app-logout', 'app-menu', 'app-entries'];

function menuPage({ user, logout = async () => {} } = {}) {
    const page = fakeDocument(MENU_IDS, ['app-user', 'app-logout', 'app-menu']);
    page.elements['app-status'].textContent = 'Loading…';
    const location = fakeLocation();
    const windowEvents = {};
    const fakeWindow = { location, addEventListener: (name, callback) => { windowEvents[name] = callback; } };
    let logouts = 0;
    const api = {
        currentUser: async () => {
            if (user instanceof Error) throw user;
            return user;
        },
        logout: async () => {
            logouts += 1;
            return logout();
        },
    };
    return {
        ...page, location, windowEvents, logouts: () => logouts,
        start: () => startMenuPage(page.document, api, entriesFor, fakeWindow),
    };
}

const users = {
    PATIENT: { id: 'patient01', displayName: 'Demo Patient One', roles: ['PATIENT'] },
    PHYSICIAN: { id: 'physician01', displayName: 'Demo Physician One', roles: ['PHYSICIAN'] },
    ADMISSIONS: { id: 'admissions01', displayName: 'Demo Admissions One', roles: ['ADMISSIONS'] },
};

for (const [role, user] of Object.entries(users)) {
    test(`a ${role.toLowerCase()} sees their name and role and no entries yet`, async () => {
        const page = menuPage({ user });
        await page.start();

        assert.equal(page.elements['app-user'].textContent, user.displayName + ' · ' + role[0] + role.slice(1).toLowerCase());
        assert.equal(page.elements['app-user'].hidden, false);
        assert.equal(page.elements['app-logout'].hidden, false);
        assert.equal(page.elements['app-menu'].hidden, false);
        assert.deepEqual(page.elements['app-entries'].links(), []);
        assert.equal(page.elements['app-status'].textContent, 'There are no pages for your role yet.');
    });
}

test('an administrator sees the roles page in the menu', async () => {
    const page = menuPage({ user: { id: 'admin01', displayName: 'Demo Administrator', roles: ['ADMINISTRATOR'] } });
    await page.start();

    const links = page.elements['app-entries'].links();
    assert.deepEqual(links.map(link => [link.href, link.textContent]), [['cu-18/index.html', 'Roles and permissions']]);
    assert.equal(page.elements['app-user'].textContent, 'Demo Administrator · Administrator');
    assert.equal(page.elements['app-status'].textContent, '');
});

test('a user with two roles sees both and the entries of both', async () => {
    const page = menuPage({ user: { id: 'physicianadmin01', displayName: 'Demo Physician Administrator',
        roles: ['PHYSICIAN', 'ADMINISTRATOR'] } });
    await page.start();

    assert.equal(page.elements['app-user'].textContent, 'Demo Physician Administrator · Physician, Administrator');
    assert.equal(page.elements['app-entries'].links().length, 1);
});

test('without a session the menu stays hidden and shows nothing (api.js goes to the login)', async () => {
    const page = menuPage({ user: NO_SESSION });
    await page.start();

    assert.equal(page.elements['app-menu'].hidden, true);
    assert.equal(page.elements['app-logout'].hidden, true);
    assert.equal(page.elements['app-status'].textContent, 'Loading…');
});

test('a server error is shown and the menu stays hidden', async () => {
    const page = menuPage({ user: apiError(0, 'unreachable', 'The server cannot be reached.') });
    await page.start();

    assert.equal(page.elements['app-status'].textContent, 'The server cannot be reached.');
    assert.equal(page.elements['app-menu'].hidden, true);
});

test('logout calls the API once', async () => {
    const page = menuPage({ user: users.PATIENT });
    await page.start();

    await page.elements['app-logout'].events.click();

    assert.equal(page.logouts(), 1);
    assert.equal(page.elements['app-logout'].disabled, true, 'api.logout navigates away; the button stays off');
});

test('a failed logout shows the error and lets the user try again', async () => {
    const page = menuPage({ user: users.PATIENT, logout: async () => {
        throw apiError(403, 'forbidden', 'You are not allowed to do this');
    } });
    await page.start();

    await page.elements['app-logout'].events.click();

    assert.equal(page.elements['app-status'].textContent, 'You are not allowed to do this');
    assert.equal(page.elements['app-logout'].disabled, false);
});

test('a menu page restored from the browser cache (back after logout) is reloaded', async () => {
    const page = menuPage({ user: users.PATIENT });
    await page.start();

    page.windowEvents.pageshow({ persisted: false });
    assert.equal(page.location.reloads, 0);
    page.windowEvents.pageshow({ persisted: true });
    assert.equal(page.location.reloads, 1);
});
