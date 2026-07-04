# E2E — dotCMS Playwright

> **Source of truth.** `CLAUDE.md` in this directory only points here.

## Layout

```
src/
  fixtures/     # auth, API setup (e.g. relationship.fixture.ts)
  pages/        # shared page objects (*.page.ts)
  utils/        # iframe, portlets, credentials
  requests/     # content type / contentlet API
  tests/edit-content/fields/{field-type}/
    helpers/    # locators for that field only
    *.spec.ts
playwright.config.ts
```

## Run

```bash
pnpm nx e2e dotcms-ui-e2e --grep "pattern"
HEADLESS=true pnpm nx e2e dotcms-ui-e2e --grep "pattern"
pnpm e2e:dev | e2e:dev:headless | e2e:ci | e2e:ui   # from core-web/
npx playwright codegen http://localhost:4200/dotAdmin
```

| | `dev` (default) | `ci` |
|---|---|---|
| URL | `:4200` (proxy → `:8080`) | `:8080` direct |

Env: `CURRENT_ENV`, `HEADLESS`, `E2E_BASE_URL`, `E2E_REUSE_EXISTING_SERVER`. Reports: `dist/.playwright/apps/dotcms-ui-e2e/`.

## Conventions

- **Files:** kebab-case; specs `{feature}-{journey}.spec.ts`; helpers in `helpers/` (no suffix); shared pages `*.page.ts`; fixtures `{feature}.fixture.ts`.
- **Tests:** `test('action description @critical')` — action only, no "should"; `@critical` / `@smoke` optional.
- **Classes:** locator wrappers named like the UI (`RelationshipField`); pages end with `Page`; no `Component` suffix.

## Locators

1. `getByRole` → 2. `getByTestId` → 3. `getByLabel` → 4. CSS **only** in Dojo iframe.

Unsure? **Codegen first.** Avoid fragile `#dijit_*` IDs, CSS when `data-testid` exists, `.locator('button')` on `getByRole('button')`, and `isVisible()` for waits (use `waitFor` / `expect().toBeVisible`).

## Angular + Dojo

- **Angular:** shell, edit form, dialogs — main `page`.
- **Dojo:** content listing in `#detailFrame` — `getLegacyFrame(page)` from `@utils/iframe`.
- **Nav:** `Portlet` from `@utils/portlets`; new content via `NewEditContentFormPage.goToNew()` (listing → New), not direct `/content/new/` URL.
- **Dojo menus:** wait for menu visibility, then click normally (no `force: true`).

Field-specific selectors and flows: copy `tests/.../helpers/` and nearest `*.spec.ts` (e.g. `relationship-field/`).

## Where to put code

| Need | Place |
|---|---|
| Cross-feature UI | `pages/` |
| One field / feature | `tests/.../helpers/` |
| Generic (iframe, URLs) | `utils/` |
| HTTP setup | `requests/` + `fixtures/` |

## Data & isolation

- Assume **empty starter** — create types/content via API in `beforeEach`, delete in `afterEach`; unique names (`testSuffix`, `Date.now()`, UUID).
- Content types need **SystemWorkflow** for Save — see existing fixtures/requests.
- **Relationships:** reuse `fixtures/relationship.fixture.ts` and `tests/edit-content/fields/relationship-field/`; do not invent payloads or cardinality numbers.

## Must-know gotchas

1. Login: `click` → `fill` email → `Tab` → then password (`userNameInput`, `password`, `submitButton`).
2. `waitForResponse` **before** the click that triggers the request.
3. Relationship/menu copy: use fixture + helpers; labels are **"Existing Content"** / **"New Content"** (verify with codegen).
4. Single-cardinality: menu items disable with `aria-disabled`; `+` button stays enabled.
5. Custom types for editor v2: `metadata.CONTENT_EDITOR2_ENABLED`; HTML pages need real `hostFolder` from `GET /api/v1/site`, not `'default'`.
6. HTML edit: no `data-testid="title"` — `goToContent()` may hang; use direct `#/content/{inode}` + wait for sidebar.
