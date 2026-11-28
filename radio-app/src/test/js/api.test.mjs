// Shared API client js/api.js and menu registry js/menu.js (F-3, CU-20-C), against a fake fetch.
// Run: node --test radio-app/src/test/js/
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { beforeEach, test } from 'node:test';

async function load(path) {
    const source = await readFile(new URL('../../main/resources/static/' + path, import.meta.url), 'utf8');
    return import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
}

const api = await load('js/api.js');
const { MENU, entriesFor } = await load('js/menu.js');

/** Fake server: answers in order, and records every call. */
let calls;
let answers;
let visited;

function answer(status, body, { raw } = {}) {
    return { status, body, raw };
}

beforeEach(() => {
    calls = [];
    answers = [];
    visited = [];
    globalThis.document = { cookie: '' };
    globalThis.window = { location: { assign: url => visited.push(url) } };
    globalThis.fetch = async (path, options) => {
        calls.push({ path, ...options });
        const next = answers.shift();
        if (next instanceof Error) throw next;
        const text = next.raw ?? (next.body === undefined ? '' : JSON.stringify(next.body));
        return {
            ok: next.status >= 200 && next.status < 300,
            status: next.status,
            text: async () => text,
            json: async () => JSON.parse(text),
        };
    };
});

test('a GET sends no CSRF header and returns the JSON of the answer', async () => {
    document.cookie = 'XSRF-TOKEN=abc';
    answers.push(answer(200, [{ id: 'physician01' }]));

    const users = await api.request('/api/users?query=phys');

    assert.deepEqual(users, [{ id: 'physician01' }]);
    assert.equal(calls[0].method, 'GET');
    assert.equal(calls[0].credentials, 'same-origin');
    assert.equal(calls[0].headers.Accept, 'application/json');
    assert.equal(calls[0].headers['X-XSRF-TOKEN'], undefined);
    assert.equal(calls[0].body, undefined);
});

for (const method of ['POST', 'PUT', 'PATCH', 'DELETE', 'put']) {
    test(`a ${method} sends the CSRF token of the cookie in the X-XSRF-TOKEN header`, async () => {
        document.cookie = 'JSESSIONID=s1; XSRF-TOKEN=6f1d%2Dtoken; theme=dark';
        answers.push(answer(204));

        await api.request('/api/users/norole01/roles/PATIENT', { method });

        assert.equal(calls[0].method, method.toUpperCase());
        assert.equal(calls[0].headers['X-XSRF-TOKEN'], '6f1d-token');
    });
}

test('a write without the cookie goes without the header (the server answers 403)', async () => {
    answers.push(answer(403, { error: 'forbidden', message: 'You are not allowed to do this' }));

    await assert.rejects(api.request('/api/auth/logout', { method: 'POST' }), { status: 403 });
    assert.equal(calls[0].headers['X-XSRF-TOKEN'], undefined);
});

test('a body goes as JSON with its content type', async () => {
    answers.push(answer(200, { ok: true }));

    await api.request('/api/things', { method: 'POST', body: { name: 'MRI' } });

    assert.equal(calls[0].body, '{"name":"MRI"}');
    assert.equal(calls[0].headers['Content-Type'], 'application/json');
});

test('an empty answer (204) gives undefined', async () => {
    answers.push(answer(204));
    assert.equal(await api.request('/api/auth/logout', { method: 'POST' }), undefined);
});

test('an error of the server becomes an ApiError with its code and message', async () => {
    answers.push(answer(400, { error: 'bad_request', message: 'The name is required' }));

    const error = await api.request('/api/things', { method: 'POST', body: {} }).catch(e => e);

    assert.ok(error instanceof api.ApiError);
    assert.equal(error.status, 400);
    assert.equal(error.error, 'bad_request');
    assert.equal(error.message, 'The name is required');
    assert.deepEqual(visited, []);
});

test('a 403 without a body still says "not allowed" and does not leave the page', async () => {
    answers.push(answer(403, undefined, { raw: '<html>Forbidden</html>' }));

    await assert.rejects(api.request('/api/users'),
        { name: 'ApiError', status: 403, error: 'http_403', message: 'You are not allowed to do this.' });
    assert.deepEqual(visited, []);
});

test('a 503 without a body gives the "not available" message', async () => {
    answers.push(answer(503, undefined, { raw: '' }));
    await assert.rejects(api.request('/api/things'),
        { status: 503, message: 'The service is not available, try again later.' });
});

test('a 401 sends the browser to the login page with the "session ended" notice', async () => {
    answers.push(answer(401, { error: 'unauthenticated', message: 'Login required' }));

    await assert.rejects(api.request('/api/users'), { status: 401, error: 'unauthenticated' });
    assert.deepEqual(visited, ['/index.html?ended=1']);
});

test('a 401 with redirectOn401 false stays on the page', async () => {
    answers.push(answer(401, { error: 'unauthenticated', message: 'Login required' }));

    await assert.rejects(api.request('/api/auth/me', { redirectOn401: false }), { status: 401 });
    assert.deepEqual(visited, []);
});

test('a server that cannot be reached gives status 0 and a clear message', async () => {
    answers.push(new TypeError('Failed to fetch'));

    await assert.rejects(api.request('/api/users'), {
        name: 'ApiError', status: 0, error: 'unreachable',
        message: 'The server cannot be reached. Check the connection and try again.',
    });
});

test('login posts the SSO token and does not redirect when it is rejected', async () => {
    answers.push(answer(401, { error: 'invalid_token', message: 'The corporate login was not accepted' }));

    await assert.rejects(api.login('token-unknown'), { status: 401, error: 'invalid_token' });
    assert.equal(calls[0].path, '/api/auth/login');
    assert.equal(calls[0].method, 'POST');
    assert.equal(calls[0].body, '{"token":"token-unknown"}');
    assert.deepEqual(visited, []);
});

test('login returns the user of the session', async () => {
    const user = { id: 'admin01', displayName: 'Demo Administrator', roles: ['ADMINISTRATOR'] };
    answers.push(answer(200, user));
    assert.deepEqual(await api.login('token-admin01'), user);
});

test('logout posts with the CSRF header and goes to the login page without the notice', async () => {
    document.cookie = 'XSRF-TOKEN=t1';
    answers.push(answer(204));

    await api.logout();

    assert.equal(calls[0].path, '/api/auth/logout');
    assert.equal(calls[0].headers['X-XSRF-TOKEN'], 't1');
    assert.deepEqual(visited, ['/index.html']);
});

test('logout with an expired session (401) goes to the login page with the notice', async () => {
    answers.push(answer(401, { error: 'unauthenticated', message: 'Login required' }));

    await api.logout();

    assert.equal(calls[0].path, '/api/auth/logout');
    assert.deepEqual(visited, ['/index.html?ended=1']);
});

test('a failed logout stays on the page and reports the error', async () => {
    answers.push(answer(403, { error: 'forbidden', message: 'You are not allowed to do this' }));

    await assert.rejects(api.logout(), { status: 403 });
    assert.deepEqual(visited, []);
});

test('requireUser gives the user when they have one of the roles', async () => {
    const user = { id: 'physicianadmin01', displayName: 'Demo Physician Administrator',
        roles: ['PHYSICIAN', 'ADMINISTRATOR'] };
    answers.push(answer(200, user));
    assert.deepEqual(await api.requireUser(['ADMINISTRATOR']), user);
});

test('requireUser rejects a user without the roles with a 403 "not allowed"', async () => {
    answers.push(answer(200, { id: 'patient01', displayName: 'Demo Patient One', roles: ['PATIENT'] }));
    await assert.rejects(api.requireUser(['ADMINISTRATOR']),
        { status: 403, error: 'forbidden', message: 'You are not allowed to do this.' });
});

test('requireUser without a session sends the browser to the login page', async () => {
    answers.push(answer(401, { error: 'unauthenticated', message: 'Login required' }));
    await assert.rejects(api.requireUser(['PATIENT']), { status: 401 });
    assert.deepEqual(visited, ['/index.html?ended=1']);
});

test('readCookie finds a cookie among others and gives null when it is missing', () => {
    assert.equal(api.readCookie('XSRF-TOKEN', 'a=1; XSRF-TOKEN=x%20y ;b=2'), 'x y');
    assert.equal(api.readCookie('XSRF-TOKEN', 'a=1; NOT-XSRF-TOKEN=z'), null);
    assert.equal(api.readCookie('XSRF-TOKEN', ''), null);
});

test('the menu shows each role only its entries', () => {
    const menu = [
        { path: 'cu-18/index.html', label: 'Roles', roles: ['ADMINISTRATOR'] },
        { path: 'cu-06/index.html', label: 'Prescribe', roles: ['PHYSICIAN'] },
        { path: 'cu-01/index.html', label: 'Appointments', roles: ['PATIENT', 'ADMISSIONS'] },
    ];
    assert.deepEqual(entriesFor(['PATIENT'], menu).map(e => e.label), ['Appointments']);
    assert.deepEqual(entriesFor(['PHYSICIAN', 'ADMINISTRATOR'], menu).map(e => e.label), ['Roles', 'Prescribe']);
    assert.deepEqual(entriesFor([], menu), []);
    assert.deepEqual(entriesFor(['UNKNOWN'], menu), []);
});

test('every entry of the real menu is one use case page with known roles', () => {
    const roles = ['PATIENT', 'PHYSICIAN', 'ADMISSIONS', 'ADMINISTRATOR'];
    const paths = MENU.map(entry => entry.path);
    assert.equal(new Set(paths).size, paths.length, 'no page twice');
    for (const entry of MENU) {
        assert.match(entry.path, /^cu-\d{2}\/index\.html$/);
        assert.ok(entry.label, entry.path);
        assert.ok(entry.roles.length > 0, entry.path);
        assert.ok(entry.roles.every(role => roles.includes(role)), entry.path);
    }
    assert.ok(Object.isFrozen(MENU));
});

test('the administration pages of iteration 1 are in the menu of administrators only', () => {
    const adminPages = ['cu-18/index.html', 'cu-23/index.html'];
    const adminPaths = entriesFor(['ADMINISTRATOR']).map(entry => entry.path);
    for (const path of adminPages) assert.ok(adminPaths.includes(path), path);
    for (const role of ['PATIENT', 'PHYSICIAN', 'ADMISSIONS']) {
        assert.deepEqual(entriesFor([role]).filter(entry => adminPages.includes(entry.path)), [], role);
    }
});
