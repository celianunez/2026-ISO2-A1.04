# Web client (static HTML/JS)

The client of SESCAM Radiodiagnostics (D-27, P-05): plain HTML, JavaScript (ES modules) and CSS served by
`radio-app`. No framework and no build step. Owner and reviewer of every page: Orhan (D-29).

| File | What it is |
|---|---|
| `index.html`, `js/login.js` | Login with the simulated corporate SSO (CU-20). |
| `app.html`, `js/app.js` | Menu after login: the entries of the user's roles, and the logout. |
| `js/api.js` | Shared client of the REST API. **Every page uses it.** |
| `js/menu.js` | The menu entries and the roles that see them. |
| `css/app.css` | Shared styles. |
| `cu-nn/` | One folder per use case client part (`CU-nn-C`). |

## Rules for every page (`CU-nn-C`)

1. **One folder per use case:** `static/cu-nn/index.html` + `static/cu-nn/cu-nn.js` (two digits: `cu-06`, `cu-18`).
2. **Use `../js/api.js` for every API call.** Never call `fetch` yourself: `api.js` adds the CSRF header, turns errors
   into `ApiError { status, error, message }`, and sends the user to the login page when the session ends (401).
3. **Register the menu entry:** one line in `js/menu.js` with the roles that may open the page.
4. **Check the role when the page opens** with `requireUser([...roles])`, and hide the actions the role cannot do.
   This is only for the user: the server checks every call again (`@PreAuthorize`). Never rely on the client.
5. **Show the server's message** (`error.message`) in an element with `role="status"` or `role="alert"`. A 403 says
   "You are not allowed to do this.": show it and keep the page working.
6. **No business logic in JavaScript** (CONTEXT.md): validate the fields before sending to help the user, but the rules
   live on the server.
7. English texts, a `<label>` for every input, a link back to the menu (`../app.html`), `../css/app.css`, and no real
   personal data in examples.

## API of `js/api.js`

| Export | Use |
|---|---|
| `request(path, { method, body, redirectOn401 })` | Any call. Returns the JSON (or `undefined` for 204). Throws `ApiError`. |
| `requireUser(roles)` | The user of the session `{ id, displayName, roles }`; 403 `ApiError` if they have none of the roles. |
| `currentUser()`, `login(token)`, `logout()` | Session (used by the login and menu pages). `logout()` goes to the login page, also when the session had already ended (401). |
| `hasAnyRole(user, roles)` | To show or hide an action. |
| `ApiError` | `status` (HTTP, 0 when the server cannot be reached), `error` (code), `message` (text for the user). |

## Minimal page

`static/cu-nn/index.html`:

```html
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My use case · SESCAM Radiodiagnostics</title>
    <link rel="stylesheet" href="../css/app.css">
</head>
<body>
    <main class="page">
        <p><a href="../app.html">Back to menu</a></p>
        <h1>My use case</h1>
        <p id="cu-nn-status" class="status" role="status" aria-live="polite">Loading…</p>
        <ul id="cu-nn-list"></ul>
    </main>
    <script type="module" src="./cu-nn.js"></script>
</body>
</html>
```

`static/cu-nn/cu-nn.js`:

```js
import {request, requireUser} from '../js/api.js';

const status = document.getElementById('cu-nn-status');
const list = document.getElementById('cu-nn-list');

try {
    await requireUser(['ADMINISTRATOR']);
    const items = await request('/api/my-resource');
    list.replaceChildren(...items.map(item => {
        const li = document.createElement('li');
        li.textContent = item.name;   // textContent, never innerHTML with server data
        return li;
    }));
    status.textContent = items.length ? '' : 'Nothing yet.';
} catch (error) {
    if (error.status !== 401) status.textContent = error.message;   // 401: api.js already went to the login
}
```

`js/menu.js`:

```js
{path: 'cu-nn/index.html', label: 'My use case', roles: ['ADMINISTRATOR']},
```

## Try it

```
mvn -B -q install -DskipTests
java -jar radio-app/target/radio-app-*.jar --spring.profiles.active=dev
```

Open <http://localhost:8080/> and log in with one of the simulated users (token = `token-` + id).
`--radio.sso.simulator.down=true` simulates the corporate SSO out of service.
