const ROLES = Object.freeze({
    PATIENT: 'Patient',
    PHYSICIAN: 'Physician',
    ADMISSIONS: 'Admissions',
    ADMINISTRATOR: 'Administrator',
});

function make(document, tag, text) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    return node;
}

export async function startRolesPage(document, request, confirmChange = message => window.confirm(message)) {
    const status = document.getElementById('roles-status');
    const admin = document.getElementById('roles-admin');
    const search = document.getElementById('roles-search');
    const query = document.getElementById('roles-query');
    const results = document.getElementById('roles-results');
    let busy = false;

    function refreshControls(disabled) {
        for (const control of admin.querySelectorAll('input, button, select')) {
            control.disabled = disabled;
        }
    }

    function showError(error) {
        if (error.status === 401 || error.status === 403) {
            admin.hidden = true;
            results.replaceChildren();
        }
        status.textContent = error.message || 'The request could not be completed.';
    }

    async function loadUsers() {
        const users = await request('/api/users?' + new URLSearchParams({query: query.value.trim()}));
        results.replaceChildren(...users.map(renderUser));
        status.textContent = users.length ? '' : 'No matching users.';
    }

    async function run(action) {
        if (busy) return;
        busy = true;
        refreshControls(true);
        try {
            await action();
        } catch (error) {
            showError(error);
        } finally {
            refreshControls(false);
            busy = false;
        }
    }

    function changeRole(user, role, method) {
        if (busy) return;
        const assign = method === 'PUT';
        const action = assign ? 'Assign' : 'Remove';
        const preposition = assign ? 'to' : 'from';
        if (!confirmChange(action + ' ' + ROLES[role] + ' ' + preposition + ' ' + user.displayName + '?')) return;
        return run(async () => {
            const path = '/api/users/' + encodeURIComponent(user.id) + '/roles/' + encodeURIComponent(role);
            await request(path, {method});
            await loadUsers();
            status.textContent = 'Role updated. The change takes effect in the user’s next session.';
        });
    }

    function renderUser(user) {
        const article = make(document, 'article');
        article.className = 'account';
        article.append(make(document, 'h2', user.displayName + ' (' + user.id + ')'));
        if (!user.active) article.append(make(document, 'p', 'Inactive account'));
        const controls = make(document, 'div');
        controls.className = 'roles';
        if (!user.roles.length) controls.append(make(document, 'span', 'No roles assigned.'));
        for (const role of user.roles) {
            const remove = make(document, 'button', 'Remove ' + (ROLES[role] || role));
            remove.type = 'button';
            remove.setAttribute('aria-label', 'Remove ' + (ROLES[role] || role) + ' from ' + user.displayName);
            remove.addEventListener('click', () => changeRole(user, role, 'DELETE'));
            controls.append(remove);
        }
        const unused = Object.keys(ROLES).filter(role => !user.roles.includes(role));
        if (unused.length) {
            const select = make(document, 'select');
            select.setAttribute('aria-label', 'New role for ' + user.displayName);
            for (const role of unused) {
                const option = make(document, 'option', ROLES[role]);
                option.value = role;
                select.append(option);
            }
            const add = make(document, 'button', 'Assign role');
            add.type = 'button';
            add.setAttribute('aria-label', 'Assign role to ' + user.displayName);
            add.addEventListener('click', () => changeRole(user, select.value, 'PUT'));
            controls.append(select, add);
        }
        article.append(controls);
        return article;
    }

    search.addEventListener('submit', event => {
        event.preventDefault();
        run(loadUsers);
    });
    try {
        const session = await request('/api/auth/me');
        if (!session.roles.includes('ADMINISTRATOR')) {
            status.textContent = 'Administrator access is required.';
            return;
        }
        // Keep the form hidden until the server has accepted the user search request.
        await loadUsers();
        admin.hidden = false;
    } catch (error) {
        showError(error);
    }
}

/** Fictitious transport to try the page without a server (`?demo=admin` or `?demo=patient`). */
export function createDemoRequest(sessionRole = 'ADMINISTRATOR') {
    const users = [
        {id: 'patient01', displayName: 'Demo Patient One', active: true, roles: ['PATIENT']},
        {id: 'physician01', displayName: 'Demo Physician One', active: true, roles: ['PHYSICIAN']},
        {id: 'norole01', displayName: 'Demo No Role', active: true, roles: []},
    ];
    const failure = (status, message) => Object.assign(new Error(message), {status});
    return async (path, {method = 'GET'} = {}) => {
        const url = new URL(path, 'https://example.invalid');
        if (url.pathname === '/api/auth/me' && method === 'GET') return {roles: [sessionRole]};
        if (sessionRole !== 'ADMINISTRATOR') throw failure(403, 'Administrator access is required.');
        if (url.pathname === '/api/users' && method === 'GET') {
            const query = (url.searchParams.get('query') || '').toLowerCase();
            return structuredClone(users.filter(user => [user.id, user.displayName]
                .some(value => value.toLowerCase().includes(query))));
        }
        const [, api, resource, id, subresource, role] = url.pathname.split('/');
        if (api !== 'api' || resource !== 'users' || subresource !== 'roles'
                || !Object.hasOwn(ROLES, role) || !['PUT', 'DELETE'].includes(method)) {
            throw failure(400, 'Invalid role request.');
        }
        const user = users.find(account => account.id === decodeURIComponent(id));
        if (!user) throw failure(404, 'User not found.');
        const updated = new Set(user.roles);
        if (method === 'PUT') updated.add(role);
        else updated.delete(role);
        user.roles = [...updated];
    };
}

if (typeof document !== 'undefined') {
    const demo = new URLSearchParams(window.location.search).get('demo');
    if (demo === 'admin' || demo === 'patient') {
        startRolesPage(document, createDemoRequest(demo === 'admin' ? 'ADMINISTRATOR' : 'PATIENT'));
    } else {
        // Dynamic import, so the unit test (which loads this file alone) does not need api.js.
        import('../js/api.js').then(({request}) => startRolesPage(document, request));
    }
}
