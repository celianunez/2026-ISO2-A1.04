/**
 * Client of CU-23 – Manage the catalogue of test types.
 *
 * The page lists the active test types and lets an administrator create, edit and deactivate them. Every call goes
 * through `../js/api.js` (injected here, so the demo and the tests share the page code). The page checks the fields
 * to help the user and shows what the server answers: the rules live on the server (README, rules 4 to 6).
 */

const BASE = '/api/catalogue/test-types';

/** Code the server accepts: upper-case letters, digits and hyphens, at most 32 characters (`TestType.requireCode`). */
const CODE = /^[A-Z0-9][A-Z0-9-]{0,31}$/;

const MODALITY_LABELS = Object.freeze({
    XRAY: 'X-ray',
    ULTRASOUND: 'Ultrasound',
    MAMMOGRAPHY: 'Mammography',
    CT: 'CT',
    MRI: 'MRI',
});

function make(document, tag, text) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    return node;
}

/**
 * What the client can see before sending: the mandatory fields, the code format and a positive duration. Empty when
 * the form can be sent. The server repeats every rule, so a wrong value still fails there.
 *
 * @param {object} data the fields of the form, with `code`
 * @returns {string[]} the problems, for the user to fix
 */
export function validate(data) {
    const missing = [];
    if (!data.code) missing.push('code');
    if (!data.name) missing.push('name');
    if (!data.modality) missing.push('modality');
    if (!data.room) missing.push('room');
    const problems = [];
    if (missing.length) problems.push('Mandatory field missing: ' + missing.join(', ') + '.');
    if (data.code && !CODE.test(data.code)) {
        problems.push('Code must contain only upper-case letters, digits and hyphens.');
    }
    if (!(data.durationMinutes > 0)) problems.push('Duration must be a positive number of minutes.');
    return problems;
}

/**
 * The session of the page checked against its roles, with the contract of `requireUser` from `../js/api.js`. The
 * browser passes that function; this default works on the injected `request`, so the demo (`?demo=`) and the tests
 * run the same page code without a server.
 *
 * @param {Function} request the transport of the page
 * @param {string[]} roles the roles that may use the page
 * @returns {Promise<object>} the user of the session
 */
async function requireRoles(request, roles) {
    const user = await request('/api/auth/me');
    if (!user.roles.some(role => roles.includes(role))) {
        throw Object.assign(new Error('You are not allowed to do this.'), {status: 403, error: 'forbidden'});
    }
    return user;
}

/**
 * Starts the page: the list of active test types and the editor for administrators.
 *
 * @param {Document} document
 * @param {Function} request the transport of `../js/api.js`, or the demo one
 * @param {object} [options]
 * @param {Function} [options.confirmChange] asked before deactivating a test type
 * @param {Function} [options.requireUser] role gate of the page; the default uses `request`
 * @returns {Promise<void>} resolved once the page is loaded or the error is shown
 */
export async function startCataloguePage(document, request, options = {}) {
    const {
        confirmChange = message => window.confirm(message),
        requireUser = roles => requireRoles(request, roles),
    } = options;
    const status = document.getElementById('cu-23-status');
    const admin = document.getElementById('cu-23-admin');
    const list = document.getElementById('cu-23-list');
    const form = document.getElementById('cu-23-form');
    const legend = document.getElementById('cu-23-legend');
    const save = document.getElementById('cu-23-save');
    const fields = {
        code: document.getElementById('cu-23-code'),
        name: document.getElementById('cu-23-name'),
        modality: document.getElementById('cu-23-modality'),
        room: document.getElementById('cu-23-room'),
        duration: document.getElementById('cu-23-duration'),
        preparation: document.getElementById('cu-23-preparation'),
        restrictions: document.getElementById('cu-23-restrictions'),
        contrast: document.getElementById('cu-23-contrast'),
        consent: document.getElementById('cu-23-consent'),
        followup: document.getElementById('cu-23-followup'),
    };
    let editing = null;
    let busy = false;

    function show(message, isError = false) {
        status.setAttribute('role', isError ? 'alert' : 'status');
        status.textContent = message;
    }

    function refreshControls(disabled) {
        for (const control of admin.querySelectorAll('input, button, select, textarea')) {
            control.disabled = disabled;
        }
    }

    function closeForm() {
        form.hidden = true;
        editing = null;
    }

    function showError(error) {
        if (error.status === 401) return;   // api.js has already sent the browser to the login page.
        if (error.status === 403) {
            admin.hidden = true;
            list.replaceChildren();
            closeForm();
        }
        show(error.message || 'The request could not be completed.', true);
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

    async function load() {
        const items = await request(BASE);
        list.replaceChildren(...items.map(renderItem));
        show(items.length ? '' : 'No active test types.');
    }

    function openForm(item) {
        if (busy) return;
        editing = item || null;
        fields.code.readOnly = Boolean(item);   // the code is the identifier and never changes.
        legend.textContent = item ? 'Edit ' + item.name : 'New test type';
        save.textContent = item ? 'Save changes' : 'Create test type';
        fields.code.value = item ? item.code : '';
        fields.name.value = item ? item.name : '';
        fields.modality.value = item ? item.modality : '';
        fields.room.value = item ? item.room : '';
        fields.duration.value = item ? String(item.durationMinutes) : '';
        fields.preparation.value = item ? item.preparationInstructions : '';
        fields.restrictions.value = item ? item.restrictions : '';
        fields.contrast.checked = Boolean(item && item.needsContrast);
        fields.consent.checked = Boolean(item && item.needsConsent);
        fields.followup.checked = Boolean(item && item.suitableForFollowUp);
        show('');
        form.hidden = false;
        fields.code.focus();
    }

    function readForm() {
        return {
            code: fields.code.value.trim().toUpperCase(),   // the server stores it upper case too.
            name: fields.name.value.trim(),
            modality: fields.modality.value,
            room: fields.room.value.trim(),
            durationMinutes: Number(fields.duration.value),
            preparationInstructions: fields.preparation.value.trim(),
            needsContrast: fields.contrast.checked,
            needsConsent: fields.consent.checked,
            restrictions: fields.restrictions.value.trim(),
            suitableForFollowUp: fields.followup.checked,
        };
    }

    /** The body of POST and PUT: the attributes only, because the code travels in the path and `active` never changes. */
    function attributesOf(data) {
        const attributes = {...data};
        delete attributes.code;
        return attributes;
    }

    function renderItem(item) {
        const card = make(document, 'article');
        card.className = 'card';
        card.append(make(document, 'h2', item.name + ' (' + item.code + ')'));
        const details = make(document, 'dl');
        const row = (term, value) => details.append(make(document, 'dt', term), make(document, 'dd', value));
        row('Modality', MODALITY_LABELS[item.modality] || item.modality);
        row('Room', item.room);
        row('Duration', item.durationMinutes + ' min');
        row('Contrast', item.needsContrast ? 'Yes' : 'No');
        row('Consent', item.needsConsent ? 'Yes' : 'No');
        row('Follow-up', item.suitableForFollowUp ? 'Yes' : 'No');
        if (item.preparationInstructions) row('Preparation', item.preparationInstructions);
        if (item.restrictions) row('Restrictions', item.restrictions);
        card.append(details);
        const actions = make(document, 'div');
        actions.className = 'actions';
        const edit = make(document, 'button', 'Edit ' + item.name);
        edit.type = 'button';
        edit.className = 'secondary';
        edit.addEventListener('click', () => openForm(item));
        const deactivate = make(document, 'button', 'Deactivate ' + item.name);
        deactivate.type = 'button';
        deactivate.addEventListener('click', () => confirmDeactivate(item));
        actions.append(edit, deactivate);
        card.append(actions);
        return card;
    }

    function confirmDeactivate(item) {
        if (busy) return;
        const question = 'Deactivate ' + item.name + ' (' + item.code
                + ')? Physicians will not be able to request it any more.';
        if (!confirmChange(question)) return;
        return run(async () => {
            await request(BASE + '/' + encodeURIComponent(item.code) + '/deactivate', {method: 'POST'});
            closeForm();
            await load();
            show('Test type deactivated: ' + item.name + '.');
        });
    }

    document.getElementById('cu-23-new').addEventListener('click', () => openForm(null));
    document.getElementById('cu-23-cancel').addEventListener('click', () => {
        if (busy) return;
        closeForm();
        show('');
    });
    form.addEventListener('submit', event => {
        event.preventDefault();
        if (busy) return;
        const data = readForm();
        const problems = validate(data);
        if (problems.length) {   // nothing is sent: the user fixes the form first.
            show(problems.join(' '), true);
            return;
        }
        return run(async () => {
            const path = BASE + '/' + encodeURIComponent(data.code);
            const message = editing
                    ? 'Test type updated: ' + data.name + '.'
                    : 'Test type created: ' + data.name + '.';
            await request(path, editing
                    ? {method: 'PUT', body: attributesOf(data)}
                    : {method: 'POST', body: attributesOf(data)});
            closeForm();
            await load();
            show(message);
        });
    });

    try {
        await requireUser(['ADMINISTRATOR']);
        // Keep everything hidden until the server has answered the list request.
        await load();
        admin.hidden = false;
    } catch (error) {
        showError(error);
    }
}

/**
 * Fictitious transport to try the page without a server (`?demo=admin` or `?demo=physician`) and the server stub of
 * the tests: it answers the catalogue the way `CatalogueController` does, including the errors.
 *
 * @param {string} [sessionRole] role of the simulated session
 * @returns {Function} a `request(path, {method, body})` like the one of `../js/api.js`
 */
export function createDemoRequest(sessionRole = 'ADMINISTRATOR') {
    const testTypes = [
        {code: 'RX-THORAX', name: 'Chest X-ray', modality: 'XRAY', room: 'RX-01', durationMinutes: 10,
            preparationInstructions: 'Remove metal objects.', needsContrast: false, needsConsent: false,
            restrictions: '', suitableForFollowUp: true, active: true},
        {code: 'US-ABDOMEN', name: 'Abdominal ultrasound', modality: 'ULTRASOUND', room: 'US-02',
            durationMinutes: 20, preparationInstructions: 'Fast for 6 hours.', needsContrast: false,
            needsConsent: false, restrictions: '', suitableForFollowUp: false, active: true},
        {code: 'CT-CHEST', name: 'Chest CT', modality: 'CT', room: 'CT-01', durationMinutes: 25,
            preparationInstructions: '', needsContrast: true, needsConsent: true,
            restrictions: 'Renal failure.', suitableForFollowUp: false, active: true},
    ];
    const failure = (status, message) => Object.assign(new Error(message), {status});
    const requireAdministrator = () => {
        if (sessionRole !== 'ADMINISTRATOR') throw failure(403, 'You are not allowed to do this.');
    };
    return async (path, {method = 'GET', body} = {}) => {
        const url = new URL(path, 'https://example.invalid');
        if (url.pathname === '/api/auth/me' && method === 'GET') {
            return {id: 'demo-user', displayName: 'Demo ' + sessionRole, roles: [sessionRole]};
        }
        const [, api, area, resource, rawCode, action] = url.pathname.split('/');
        if (api !== 'api' || area !== 'catalogue' || resource !== 'test-types') {
            throw failure(400, 'Invalid catalogue request.');
        }
        const code = rawCode === undefined ? undefined : decodeURIComponent(rawCode);
        const index = testTypes.findIndex(item => item.code === code);
        if (method === 'GET' && code === undefined) {   // the list is open to every authenticated user.
            return structuredClone(testTypes.filter(item => item.active));
        }
        if (method === 'GET') {
            if (index < 0) throw failure(404, 'No test type with code ' + code);
            return structuredClone(testTypes[index]);
        }
        requireAdministrator();
        if (method === 'POST' && action === 'deactivate') {
            if (index < 0) throw failure(404, 'No test type with code ' + code);
            testTypes[index].active = false;
            return undefined;   // HTTP 204, as the controller answers it.
        }
        if (method === 'POST' && action === undefined) {
            if (index >= 0) throw failure(400, 'A test type with code ' + code + ' already exists');
            const problems = validate({...body, code});   // the server repeats every check of the client.
            if (problems.length) throw failure(400, problems.join(' '));
            testTypes.push({...body, code, active: true});
            return structuredClone(testTypes[testTypes.length - 1]);
        }
        if (method === 'PUT' && action === undefined) {
            if (index < 0) throw failure(404, 'No test type with code ' + code);
            const problems = validate({...body, code});
            if (problems.length) throw failure(400, problems.join(' '));
            const stored = testTypes[index];
            testTypes[index] = {...stored, ...body, code: stored.code, active: stored.active};
            return structuredClone(testTypes[index]);
        }
        throw failure(400, 'Invalid catalogue request.');
    };
}

if (typeof document !== 'undefined') {
    const demo = new URLSearchParams(window.location.search).get('demo');
    if (demo === 'admin' || demo === 'physician') {
        startCataloguePage(document, createDemoRequest(demo === 'admin' ? 'ADMINISTRATOR' : 'PHYSICIAN'));
    } else {
        // Dynamic import, so the unit test (which loads this file alone) does not need api.js.
        import('../js/api.js').then(api => startCataloguePage(document, api.request, {requireUser: api.requireUser}));
    }
}
