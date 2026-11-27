/**
 * Shared client of the REST API (F-3). Every page imports it (`../js/api.js` from `cu-nn/`).
 *
 * - The session cookie travels on its own (same origin).
 * - POST, PUT, PATCH and DELETE send the CSRF token: the value of the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN`
 *   header (the server renews it on login and clears it on logout).
 * - Errors become an {@link ApiError} with the `{ error, message }` of the server.
 * - 401: the session is over, so the browser goes to the login page (unless the caller turns it off).
 *   `logout()` also goes there on 401: the session had already ended.
 * - 403: the error carries the "not allowed" message, for the page to show it.
 *
 * No business logic here: pages show data and send what the user typed; the server decides.
 */

export const LOGIN_PAGE = '/index.html';
export const MENU_PAGE = '/app.html';

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

const DEFAULT_MESSAGES = Object.freeze({
    0: 'The server cannot be reached. Check the connection and try again.',
    401: 'Your session has ended. Log in again.',
    403: 'You are not allowed to do this.',
    404: 'Not found.',
    503: 'The service is not available, try again later.',
});

/** An answer of the API that is not 2xx (or no answer at all: `status` 0). */
export class ApiError extends Error {
    /**
     * @param {number} status  HTTP status, 0 when the server could not be reached
     * @param {string} error   machine code of the server (`invalid_token`, `forbidden`…)
     * @param {string} message text for the user
     */
    constructor(status, error, message) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.error = error;
    }
}

/**
 * Value of a cookie, or `null`.
 *
 * @param {string} name
 * @param {string} [cookies] the `document.cookie` string
 */
export function readCookie(name, cookies = globalThis.document?.cookie ?? '') {
    for (const pair of cookies.split(';')) {
        const separator = pair.indexOf('=');
        if (separator < 0) continue;
        if (pair.slice(0, separator).trim() === name) {
            return decodeURIComponent(pair.slice(separator + 1).trim());
        }
    }
    return null;
}

/** Sends the browser to the login page. `ended` adds the "session ended" notice. */
export function goToLogin(ended = true) {
    window.location.assign(ended ? LOGIN_PAGE + '?ended=1' : LOGIN_PAGE);
}

/**
 * Calls the API and returns the JSON of the answer (`undefined` for an empty answer, such as 204).
 *
 * @param {string} path            for example `/api/users?query=ann`
 * @param {object} [options]
 * @param {string} [options.method]          GET by default
 * @param {*}      [options.body]            sent as JSON
 * @param {boolean} [options.redirectOn401]  true by default: a 401 sends the browser to the login page
 * @returns {Promise<*>}
 * @throws {ApiError} on any answer that is not 2xx
 */
export async function request(path, {method = 'GET', body, redirectOn401 = true} = {}) {
    const verb = method.toUpperCase();
    const headers = {Accept: 'application/json'};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (UNSAFE_METHODS.has(verb)) {
        const token = readCookie(CSRF_COOKIE);
        if (token) headers[CSRF_HEADER] = token;
    }

    let response;
    try {
        response = await fetch(path, {
            method: verb,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
            credentials: 'same-origin',
        });
    } catch {
        throw new ApiError(0, 'unreachable', DEFAULT_MESSAGES[0]);
    }

    if (response.ok) {
        const text = response.status === 204 ? '' : await response.text();
        return text ? JSON.parse(text) : undefined;
    }

    const payload = await response.json().catch(() => ({}));
    const error = new ApiError(response.status, payload.error || 'http_' + response.status,
        payload.message || DEFAULT_MESSAGES[response.status] || 'The request could not be completed.');
    if (response.status === 401 && redirectOn401) goToLogin();
    throw error;
}

/**
 * Logs in with a token of the corporate SSO (simulated: `token-<userId>`).
 *
 * @returns {Promise<{id: string, displayName: string, roles: string[]}>}
 * @throws {ApiError} 401 `invalid_token`, 403 `no_role`, 503 `sso_unavailable`
 */
export function login(token) {
    return request('/api/auth/login', {method: 'POST', body: {token}, redirectOn401: false});
}

/**
 * The user of the session.
 *
 * @param {object} [options] `{ redirectOn401 }`, true by default
 * @returns {Promise<{id: string, displayName: string, roles: string[]}>}
 */
export function currentUser({redirectOn401 = true} = {}) {
    return request('/api/auth/me', {redirectOn401});
}

/**
 * Ends the session and goes to the login page. If the session had already ended (401), it goes to the login page too,
 * with the "session ended" notice. Any other error is thrown, so the menu stays open and shows it.
 */
export async function logout() {
    try {
        await request('/api/auth/logout', {method: 'POST', redirectOn401: false});
    } catch (error) {
        if (error.status !== 401) throw error;
        goToLogin();
        return;
    }
    goToLogin(false);
}

/** True when the user has at least one of the roles. */
export function hasAnyRole(user, roles) {
    return roles.some(role => user.roles.includes(role));
}

/**
 * For the pages of the use cases: the user of the session, checked against the roles of the page. Without a session
 * the browser goes to the login page; with none of the roles it throws a 403 {@link ApiError}. This only hides the
 * page: the server checks the permission again on every call.
 *
 * @param {string[]} roles the roles that may use the page
 */
export async function requireUser(roles) {
    const user = await currentUser();
    if (!hasAnyRole(user, roles)) {
        throw new ApiError(403, 'forbidden', DEFAULT_MESSAGES[403]);
    }
    return user;
}
