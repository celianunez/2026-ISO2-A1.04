import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const source = await readFile(
    new URL('../../main/resources/static/cu-23/cu-23.js', import.meta.url), 'utf8',
);
const { createDemoRequest, startCataloguePage, validate } = await import(
    'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
);

const TAGS = {
    'cu-23-status': 'p',
    'cu-23-admin': 'section',
    'cu-23-list': 'ul',
    'cu-23-form': 'form',
    'cu-23-legend': 'legend',
    'cu-23-new': 'button',
    'cu-23-save': 'button',
    'cu-23-cancel': 'button',
    'cu-23-code': 'input',
    'cu-23-name': 'input',
    'cu-23-modality': 'select',
    'cu-23-room': 'input',
    'cu-23-duration': 'input',
    'cu-23-preparation': 'input',
    'cu-23-restrictions': 'input',
    'cu-23-contrast': 'input',
    'cu-23-consent': 'input',
    'cu-23-followup': 'input',
};

class Element {
    children = [];
    events = {};
    attributes = {};
    value = '';
    checked = false;
    textContent = '';
    hidden = false;
    readOnly = false;
    disabled = false;
    className = '';
    type = '';

    constructor(tag) { this.tag = tag; }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this.children = nodes; }
    setAttribute(name, value) { this.attributes[name] = value; }
    addEventListener(name, callback) { this.events[name] = callback; }
    focus() {}

    descendants() {
        return this.children.flatMap(child => [child, ...child.descendants()]);
    }

    querySelectorAll(tags) {
        return this.descendants().filter(child => tags.split(', ').includes(child.tag));
    }
}

function fixture() {
    const el = Object.fromEntries(Object.entries(TAGS).map(([id, tag]) => [id, new Element(tag)]));
    el['cu-23-admin'].hidden = true;
    el['cu-23-form'].hidden = true;
    el['cu-23-status'].textContent = 'Loading…';   // the text of the page before the first answer
    el['cu-23-form'].append(
        el['cu-23-legend'], el['cu-23-code'], el['cu-23-name'], el['cu-23-modality'], el['cu-23-room'],
        el['cu-23-duration'], el['cu-23-preparation'], el['cu-23-restrictions'], el['cu-23-contrast'],
        el['cu-23-consent'], el['cu-23-followup'], el['cu-23-save'], el['cu-23-cancel'],
    );
    el['cu-23-admin'].append(el['cu-23-new'], el['cu-23-form'], el['cu-23-list']);
    const document = {
        createElement: tag => new Element(tag),
        getElementById: id => el[id],
    };
    const button = label => el['cu-23-admin'].descendants()
        .find(node => node.tag === 'button' && node.textContent === label);
    const submit = () => el['cu-23-form'].events.submit({preventDefault() {}});
    const cards = () => el['cu-23-list'].children;
    return { document, el, status: el['cu-23-status'], admin: el['cu-23-admin'], form: el['cu-23-form'],
        list: el['cu-23-list'], button, submit, cards };
}

/** Fills the form of a new test type; `overrides` leaves a field empty to test the mandatory checks. */
function fill(page, overrides = {}) {
    const values = {
        code: 'MRI-BRAIN',
        name: 'Brain MRI',
        modality: 'MRI',
        room: 'RM-01',
        duration: '30',
        preparation: 'Remove jewellery.',
        restrictions: '',
        ...overrides,
    };
    for (const [id, value] of Object.entries(values)) page.el['cu-23-' + id].value = value;
    page.el['cu-23-consent'].checked = true;
}

test('an administrator creates a test type with the mandatory fields and it appears in the catalogue', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    await startCataloguePage(page.document, demo);

    assert.equal(page.admin.hidden, false);
    assert.equal(page.cards().length, 3);

    page.el['cu-23-new'].events.click();
    assert.equal(page.form.hidden, false);
    fill(page);
    await page.submit();

    assert.equal(page.form.hidden, true);
    assert.equal(page.cards().length, 4);
    assert.equal(page.cards()[3].descendants().find(node => node.tag === 'h2').textContent,
        'Brain MRI (MRI-BRAIN)');
    assert.match(page.status.textContent, /created: Brain MRI/);
    const stored = (await demo('/api/catalogue/test-types')).at(-1);
    assert.equal(stored.code, 'MRI-BRAIN');
    assert.equal(stored.active, true);
});

test('a missing mandatory field is rejected on the client and no request is sent', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const requests = [];
    await startCataloguePage(page.document, (path, options) => {
        requests.push((options?.method || 'GET') + ' ' + path);
        return demo(path, options);
    });

    page.el['cu-23-new'].events.click();
    fill(page, {room: ''});
    await page.submit();

    assert.equal(page.status.attributes.role, 'alert');
    assert.match(page.status.textContent, /Mandatory field missing: room/);
    assert.equal(page.form.hidden, false);
    assert.deepEqual(requests, ['GET /api/auth/me', 'GET /api/catalogue/test-types']);
    assert.equal(page.cards().length, 3);

    assert.deepEqual(validate({code: 'T01', name: 'Test', modality: 'CT', room: 'CT-01', durationMinutes: 0}),
        ['Duration must be a positive number of minutes.']);
    assert.deepEqual(validate({code: 'bad code', name: 'Test', modality: 'CT', room: 'CT-01', durationMinutes: 5}),
        ['Code must contain only upper-case letters, digits and hyphens.']);
});

test('a repeated code is rejected by the server and the list does not change', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    await startCataloguePage(page.document, demo);

    page.el['cu-23-new'].events.click();
    fill(page, {code: 'RX-THORAX', name: 'Another chest X-ray'});
    await page.submit();

    assert.equal(page.status.attributes.role, 'alert');
    assert.match(page.status.textContent, /already exists/);
    assert.equal(page.form.hidden, false);
    assert.equal(page.cards().length, 3);
    assert.equal((await demo('/api/catalogue/test-types')).length, 3);
});

test('editing sends a PUT with the same code and keeps the active flag', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const writes = [];
    await startCataloguePage(page.document, (path, options) => {
        if (options?.method && options.method !== 'GET') writes.push(options.method + ' ' + path);
        return demo(path, options);
    });

    await page.button('Edit Chest X-ray').events.click();
    assert.equal(page.el['cu-23-code'].readOnly, true);
    assert.equal(page.el['cu-23-modality'].value, 'XRAY');
    page.el['cu-23-name'].value = 'Chest X-ray (PA)';
    await page.submit();

    assert.deepEqual(writes, ['PUT /api/catalogue/test-types/RX-THORAX']);
    const stored = (await demo('/api/catalogue/test-types')).find(item => item.code === 'RX-THORAX');
    assert.equal(stored.name, 'Chest X-ray (PA)');
    assert.equal(stored.active, true);
    assert.equal(page.cards().length, 3);
    assert.match(page.status.textContent, /updated: Chest X-ray \(PA\)/);
});

test('cancelling the confirmation of a deactivation sends no request', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const writes = [];
    const confirmations = [];
    await startCataloguePage(page.document, (path, options) => {
        if (options?.method && options.method !== 'GET') writes.push(options.method + ' ' + path);
        return demo(path, options);
    }, {
        confirmChange: message => {
            confirmations.push(message);
            return false;
        },
    });

    await page.button('Deactivate Chest X-ray').events.click();

    assert.deepEqual(writes, []);
    assert.equal(page.cards().length, 3);
    assert.equal(confirmations.length, 1);
    assert.match(confirmations[0], /Chest X-ray \(RX-THORAX\)/);
    assert.equal((await demo('/api/catalogue/test-types')).some(item => item.code === 'RX-THORAX'), true);
});

test('confirming a deactivation takes the test type out of the catalogue', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    await startCataloguePage(page.document, demo, {confirmChange: () => true});

    await page.button('Deactivate Chest X-ray').events.click();

    assert.equal(page.cards().length, 2);
    assert.match(page.status.textContent, /deactivated: Chest X-ray/);
    assert.equal((await demo('/api/catalogue/test-types')).some(item => item.code === 'RX-THORAX'), false);
    assert.equal((await demo('/api/catalogue/test-types/RX-THORAX')).active, false);
});

test('a session without the administrator role sees the message of the server and no actions', async () => {
    const page = fixture();
    const demo = createDemoRequest('PHYSICIAN');
    const requests = [];
    await startCataloguePage(page.document, (path, options) => {
        requests.push(path);
        return demo(path, options);
    });

    assert.equal(page.admin.hidden, true);
    assert.equal(page.cards().length, 0);
    assert.equal(page.status.attributes.role, 'alert');
    assert.equal(page.status.textContent, 'You are not allowed to do this.');
    assert.deepEqual(requests, ['/api/auth/me']);
    await assert.rejects(demo('/api/catalogue/test-types/RX-THORAX/deactivate', {method: 'POST'}),
        {status: 403});
});

test('a finished session leaves api.js sending the browser to the login page', async () => {
    const page = fixture();
    const ended = Object.assign(new Error('Your session has ended. Log in again.'), {status: 401});
    const requests = [];
    await startCataloguePage(page.document, async path => {
        requests.push(path);
        throw ended;
    });

    assert.deepEqual(requests, ['/api/auth/me']);
    assert.equal(page.admin.hidden, true);
    assert.equal(page.cards().length, 0);
    assert.equal(page.status.textContent, 'Loading…');
});
