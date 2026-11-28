import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const source = await readFile(
    new URL('../../main/resources/static/cu-18/cu-18.js', import.meta.url), 'utf8',
);
const { createDemoRequest, startRolesPage } = await import(
    'data:text/javascript;base64,' + Buffer.from(source).toString('base64')
);

class Element {
    children = [];
    events = {};
    attributes = {};
    value = '';
    textContent = '';
    hidden = false;

    constructor(tag) { this.tag = tag; }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this.children = nodes; }
    setAttribute(name, value) { this.attributes[name] = value; }
    addEventListener(name, callback) { this.events[name] = callback; }

    descendants() {
        return this.children.flatMap(child => [child, ...child.descendants()]);
    }

    querySelectorAll(tags) {
        return this.descendants().filter(child => tags.split(', ').includes(child.tag));
    }
}

function fixture() {
    const admin = new Element('section');
    admin.hidden = true;
    const search = new Element('form');
    const query = new Element('input');
    const results = new Element('div');
    const status = new Element('p');
    search.append(query);
    admin.append(search, results);
    const elements = {
        'roles-admin': admin,
        'roles-search': search,
        'roles-query': query,
        'roles-results': results,
        'roles-status': status,
    };
    const document = {
        createElement: tag => new Element(tag),
        getElementById: id => elements[id],
    };
    const button = label => results.descendants().find(node => node.attributes['aria-label'] === label);
    return { document, admin, search, query, results, status, button };
}

test('an administrator can assign and remove a role after confirming', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const confirmations = [];
    await startRolesPage(page.document, demo, message => {
        confirmations.push(message);
        return true;
    });

    assert.equal(page.admin.hidden, false);
    const chooser = page.results.descendants().find(node => node.tag === 'select');
    chooser.value = 'PHYSICIAN';
    await page.button('Assign role to Demo Patient One').events.click();
    assert.match(page.status.textContent, /next session/);
    assert.deepEqual((await demo('/api/users?query=patient01'))[0].roles, ['PATIENT', 'PHYSICIAN']);

    await page.button('Remove Physician from Demo Patient One').events.click();
    assert.deepEqual((await demo('/api/users?query=patient01'))[0].roles, ['PATIENT']);
    assert.equal(confirmations.length, 2);
});

test('cancelling a role change makes no write request', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const writes = [];
    await startRolesPage(page.document, (path, options) => {
        if (options?.method) writes.push(options.method);
        return demo(path, options);
    }, () => false);

    await page.button('Remove Patient from Demo Patient One').events.click();

    assert.deepEqual(writes, []);
    assert.deepEqual((await demo('/api/users?query=patient01'))[0].roles, ['PATIENT']);
});

test('patient sessions never see or load the user controls', async () => {
    const page = fixture();
    const demo = createDemoRequest('PATIENT');
    const requests = [];
    await startRolesPage(page.document, (path, options) => {
        requests.push(path);
        return demo(path, options);
    });

    assert.equal(page.admin.hidden, true);
    assert.equal(page.results.children.length, 0);
    assert.deepEqual(requests, ['/api/auth/me']);
    await assert.rejects(demo('/api/users'), {status: 403});
});

test('the demo searches by corporate ID or display name without leaking mutable records', async () => {
    const demo = createDemoRequest();
    const byId = await demo('/api/users?query=PATIENT01');
    assert.equal(byId[0].displayName, 'Demo Patient One');
    byId[0].roles.length = 0;

    assert.deepEqual((await demo('/api/users?query=patient%20one'))[0].roles, ['PATIENT']);
    assert.deepEqual(await demo('/api/users?query=absent'), []);
    await assert.rejects(demo('/api/users/absent/roles/PATIENT', {method: 'DELETE'}), {status: 404});
});

test('a missing account leaves the role editor open and shows the server error', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const message = 'User not found.';
    await startRolesPage(page.document, (path, options) => {
        if (path === '/api/users/patient01/roles/PATIENT' && options?.method === 'DELETE') {
            return Promise.reject(Object.assign(new Error(message), {status: 404}));
        }
        return demo(path, options);
    }, () => true);

    const remove = page.button('Remove Patient from Demo Patient One');
    await remove.events.click();

    assert.equal(page.status.textContent, message);
    assert.equal(page.admin.hidden, false);
    assert.equal(remove.disabled, false);
});

test('a forbidden role change hides the editor and clears the displayed accounts', async () => {
    const page = fixture();
    const demo = createDemoRequest();
    const message = 'You are not allowed to do this.';
    await startRolesPage(page.document, (path, options) => {
        if (path === '/api/users/patient01/roles/PATIENT' && options?.method === 'DELETE') {
            return Promise.reject(Object.assign(new Error(message), {status: 403}));
        }
        return demo(path, options);
    }, () => true);

    assert.equal(page.admin.hidden, false);
    await page.button('Remove Patient from Demo Patient One').events.click();

    assert.equal(page.status.textContent, message);
    assert.equal(page.admin.hidden, true);
    assert.equal(page.results.children.length, 0);
});
