# Contract: `/api/v1/configuration` changes

Every endpoint below rejects with **401 `text/plain`**: when there is no user, when the user is
not a backend user, or (where required) when the user is not a CMS Administrator. That status is
unchanged; only the docs change, from 403 to 401.

## Gate change (no behaviour change)

| Endpoint | Before | After |
|----------|--------|-------|
| `GET /branding`, `PUT /branding`, `PUT /authentication`, `PUT /locale`, `POST /_regenerateKey`, `POST /_validateCompanyEmail` | CMS Admin + portlet `maintenance` | CMS Admin + portlet `configuration` |

## `PUT /branding` and `GET /branding` — `backgroundImage`

Request and response shapes are unchanged. Only the accepted and returned values change.

| `backgroundImage` sent | Before | After |
|------------------------|--------|-------|
| absent / `null` / `""` | 200, stored blank | same |
| `/dA/...` | 200, stored | same |
| `/html/images/backgrounds/bg-1.jpg` … `bg-11.jpg` | **400** | 200, stored |
| equal to the stored value (any) | 400 unless `/dA` | 200, kept |
| anything else (e.g. `bg-12.jpg`, `BG-1.JPG`, `/html/images/backgrounds/bg-1.jpg?x`, `https://x/bg.jpg`) | 400 | 400; the message names `backgroundImage` and the accepted forms |

| Stored `homeURL` | `backgroundImage` returned before | After |
|------------------|-----------------------------------|-------|
| `/dA/...` | value | same |
| preset or other URL | `null` | value |
| blank or `localhost` | `null` | same |

## `POST /_validateCompanyEmail`

| Request | Before | After |
|---------|--------|-------|
| JSON `{"senderAndEmail": "<valid>"}` | 200 `{"entity":"Ok", ...}` | same |
| JSON `{"senderAndEmail": "broken"}` | 400 `{"message":"input does not match a valid e-mail pattern."}` | same |
| no body | **500** | 400, "Request body is required" |

Adds `@Consumes(application/json)` and full OpenAPI docs. Every known caller already sends JSON.

## NEW `GET /license`

- **Auth**: any backend user. Front-end-only and anonymous → 401. No role, portlet or
  license-level check.
- **200** response:

```json
{
  "entity": {
    "title": "dotCMS Business Source License 1.1",
    "licensor": "dotCMS LLC",
    "changeDate": "Four years from August 01, 2025",
    "changeLicense": "GNU General Public License (GPL) v3",
    "text": "Licensor:             dotCMS LLC\n\n..."
  },
  "errors": [], "i18nMessagesMap": {}, "messages": [], "pagination": null, "permissions": []
}
```

- **Unreadable file**: still 200. `text` holds the fallback sentence, `title` is
  "dotCMS Business Source License", and the other fields are `null`.
