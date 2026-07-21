# E2E — dotCMS Playwright

> **Source of truth for code conventions.** `CLAUDE.md` in this directory points here.

**How to run tests, CI parity, unit vs E2E cheat sheet, and AI workflow:** see [README.md](README.md).

## Layout

```
src/
  fixtures/     # auth, API setup (e.g. relationship.fixture.ts)
  pages/        # shared page objects (*.page.ts)
  utils/        # iframe, portlets, credentials
  requests/     # REST API helpers for test data (content types, contentlets, sites, …)
  tests/edit-content/fields/{field-type}/
    helpers/    # locators for that field only
    *.spec.ts
playwright.config.ts
```

## Conventions

- **Files:** kebab-case; specs `{feature}-{journey}.spec.ts`; helpers in `helpers/` (no suffix); shared pages `*.page.ts`; fixtures `{feature}.fixture.ts`.
- **Tests:** `test('action description @critical')` — action only, no "should"; `@critical` / `@smoke` optional.
- **Classes:** locator wrappers named like the UI (`RelationshipField`); pages end with `Page`; no `Component` suffix.

## Locators

1. `getByRole` → 2. `getByTestId` → 3. `getByLabel` → 4. CSS **only** in Dojo iframe.

Unsure? **Codegen first** (`npx playwright codegen http://localhost:4200/dotAdmin`). Avoid fragile `#dijit_*` IDs, CSS when `data-testid` exists, `.locator('button')` on `getByRole('button')`, and `isVisible()` for waits (use `waitFor` / `expect().toBeVisible`).

## Angular + Dojo

- **Angular:** shell, edit form, dialogs — main `page`.
- **Dojo:** content listing in `#detailFrame` — `getLegacyFrame(page)` from `@utils/iframe`.
- **Nav:** `Portlet` from `@utils/portlets`; new content via `NewEditContentFormPage.goToNew()` (listing → New), not direct `/content/new/` URL.
- **Dojo menus:** wait for menu visibility, then click normally (no `force: true`).

Field-specific selectors and flows: copy `tests/.../helpers/` and nearest `*.spec.ts` (e.g. `relationship-field/`, `key-value-field/`).

## Where to put code

| Need | Place |
|---|---|
| Cross-feature UI | `pages/` |
| One field / feature | `tests/.../helpers/` |
| Generic (iframe, URLs) | `utils/` |
| HTTP setup | `requests/` + `fixtures/` |

## REST API setup (`src/requests/`)

**Always create test data through the REST API** — never via the UI during setup. Specs and fixtures import helpers from `src/requests/`; raw endpoint calls live there, not in test files.

Modules: `contentType.ts`, `contentlets.ts`, `sites.ts`, `folders.ts`, `pages.ts`, `templates.ts`, `schemas.ts`, `workflow.ts`, `workflowActions.ts`, `field-variables.ts`, `updateFeatureFlag.ts`.

**Adding a new helper:**

1. Search `src/requests/` first — reuse an existing function if it covers your endpoint/payload.
2. Do not duplicate the same API call in a spec, fixture, or another request file.
3. If missing, add to the appropriate `src/requests/*.ts` file so all specs can reuse it.
4. Multi-step orchestration → `fixtures/`; single-endpoint calls → `requests/`.

## Data & isolation

- Assume **empty starter** — create types/content via API helpers in `src/requests/` inside `beforeEach`, delete in `afterEach`; unique names (`testSuffix`, `Date.now()`, UUID).
- Content types need **SystemWorkflow** for Save — see existing fixtures/requests.
- **Relationships:** reuse `fixtures/relationship.fixture.ts` and `tests/edit-content/fields/relationship-field/`; do not invent payloads or cardinality numbers.

## Must-know gotchas

1. Login: `click` → `fill` email → `Tab` → then password (`userNameInput`, `password`, `submitButton`).
2. `waitForResponse` **before** the click that triggers the request.
3. Relationship/menu copy: use fixture + helpers; labels are **"Existing Content"** / **"New Content"** (verify with codegen).
4. Single-cardinality: menu items disable with `aria-disabled`; `+` button stays enabled.
5. Custom types for editor v2: `metadata.CONTENT_EDITOR2_ENABLED`; HTML pages need real `hostFolder` from `GET /api/v1/site`, not `'default'`.
6. HTML edit: no `data-testid="title"` — `goToContent()` may hang; use direct `#/content/{inode}` + wait for sidebar.
